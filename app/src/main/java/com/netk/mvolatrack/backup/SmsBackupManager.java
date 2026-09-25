package com.netk.mvolatrack.backup;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.netk.mvolatrack.database.AppDatabase;
import com.netk.mvolatrack.database.SmsMessage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Coordinates encrypted Room exports in shared storage, which survives app removal. */
public final class SmsBackupManager {
    public static final String FILE_NAME = "sms-history.smsbackup";
    private static final String TAG = "SmsBackupManager";
    private static final String BACKUP_EXTENSION = ".smsbackup";
    private static final String DIRECTORY_NAME = "SmsTracker";
    private static final String RELATIVE_DIRECTORY = Environment.DIRECTORY_DOWNLOADS + "/" + DIRECTORY_NAME + "/";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private final Context context;
    private final PasswordVault vault;

    public SmsBackupManager(Context context) {
        this.context = context.getApplicationContext();
        vault = new PasswordVault(this.context);
    }

    public interface Callback<T> { void complete(T result, Exception error); }

    public enum AutomaticRestoreStatus { RESTORED, PASSWORD_REQUIRED, NO_BACKUP }

    public static final class AutomaticRestoreResult {
        public final AutomaticRestoreStatus status;
        public final int restoredCount;

        private AutomaticRestoreResult(AutomaticRestoreStatus status, int restoredCount) {
            this.status = status;
            this.restoredCount = restoredCount;
        }
    }

    public void backup(char[] password, boolean rememberForAutomaticBackup, Callback<Date> callback) {
        char[] passwordCopy = Arrays.copyOf(password, password.length);
        EXECUTOR.execute(() -> {
            try {
                List<SmsMessage> messages = AppDatabase.getInstance(context).smsDao().getAllNewestFirst();
                byte[] encoded = BackupCodec.encode(messages, passwordCopy);
                writeBackup(encoded);
                if (rememberForAutomaticBackup) vault.save(passwordCopy);
                callback.complete(new Date(), null);
            } catch (Exception error) { callback.complete(null, error); }
            finally { Arrays.fill(passwordCopy, '\0'); }
        });
    }

    /** Does nothing until the user has created a manual backup and opted into password storage. */
    public void automaticBackup() {
        char[] password = vault.load();
        if (password == null) return;
        backup(password, false, (ignored, error) -> { });
        Arrays.fill(password, '\0');
    }

    public void restore(char[] password, Callback<Integer> callback) {
        char[] passwordCopy = Arrays.copyOf(password, password.length);
        EXECUTOR.execute(() -> {
            try {
                DecodedBackup backup = findNewestDecodableBackup(passwordCopy);
                if (backup == null) throw new BackupCodec.BackupException("Aucune sauvegarde trouvée");
                List<Long> insertions = AppDatabase.getInstance(context).smsDao().insertAll(backup.messages);
                int count = countInserted(insertions);
                vault.save(passwordCopy);
                Log.i(TAG, "Restauration : " + count + " SMS");
                callback.complete(count, null);
            } catch (Exception error) { callback.complete(null, error); }
            finally { Arrays.fill(passwordCopy, '\0'); }
        });
    }

    /** Restores without interaction when this installation still owns the Keystore password. */
    public void restoreAutomatically(Callback<AutomaticRestoreResult> callback) {
        EXECUTOR.execute(() -> {
            char[] password = null;
            try {
                List<BackupFile> files = listValidBackupEnvelopes();
                if (files.isEmpty()) {
                    callback.complete(new AutomaticRestoreResult(AutomaticRestoreStatus.NO_BACKUP, 0), null);
                    return;
                }
                password = vault.load();
                if (password == null) {
                    callback.complete(new AutomaticRestoreResult(AutomaticRestoreStatus.PASSWORD_REQUIRED, 0), null);
                    return;
                }
                DecodedBackup backup = findNewestDecodableBackup(files, password);
                if (backup == null) throw new BackupCodec.BackupException("Aucune sauvegarde valide trouvée");
                List<Long> insertions = AppDatabase.getInstance(context).smsDao().insertAll(backup.messages);
                int count = countInserted(insertions);
                Log.i(TAG, "Restauration : " + count + " SMS");
                callback.complete(new AutomaticRestoreResult(AutomaticRestoreStatus.RESTORED, count), null);
            } catch (Exception error) {
                callback.complete(null, error);
            } finally {
                if (password != null) Arrays.fill(password, '\0');
            }
        });
    }

    public void findBackup(Callback<Date> callback) {
        EXECUTOR.execute(() -> {
            try {
                List<BackupFile> files = listValidBackupEnvelopes();
                callback.complete(files.isEmpty() ? null : new Date(files.get(0).modified), null);
            } catch (Exception error) { callback.complete(null, error); }
        });
    }

    private static int countInserted(List<Long> insertionResults) {
        int inserted = 0;
        for (Long result : insertionResults) if (result != null && result != -1L) inserted++;
        return inserted;
    }

    private void writeBackup(byte[] data) throws Exception {
        List<BackupFile> oldBackups = listBackupFiles();
        for (BackupFile oldBackup : oldBackups) delete(oldBackup);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream");
            values.put(MediaStore.Downloads.RELATIVE_PATH, RELATIVE_DIRECTORY);
            values.put(MediaStore.Downloads.IS_PENDING, 1);
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new java.io.IOException("Impossible de créer le fichier de sauvegarde");
            boolean complete = false;
            try {
                try (OutputStream output = resolver.openOutputStream(uri, "w")) {
                    if (output == null) throw new java.io.IOException("Impossible d'ouvrir la sauvegarde");
                    output.write(data);
                }
                ContentValues ready = new ContentValues();
                ready.put(MediaStore.Downloads.IS_PENDING, 0);
                ready.put(MediaStore.Downloads.DATE_MODIFIED, System.currentTimeMillis() / 1000L);
                if (resolver.update(uri, ready, null, null) != 1) {
                    throw new java.io.IOException("Impossible de finaliser la sauvegarde");
                }
                complete = true;
            } finally {
                if (!complete) resolver.delete(uri, null, null);
            }
        } else {
            File target = legacyFile(FILE_NAME);
            File parent = target.getParentFile();
            if (parent == null || (!parent.exists() && !parent.mkdirs()))
                throw new java.io.IOException("Dossier de sauvegarde inaccessible");
            File temporary = new File(parent, FILE_NAME + ".tmp");
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                output.write(data);
                output.getFD().sync();
            }
            if (!temporary.renameTo(target)) throw new java.io.IOException("Impossible de finaliser la sauvegarde");
        }
    }

    private DecodedBackup findNewestDecodableBackup(char[] password) throws Exception {
        return findNewestDecodableBackup(listValidBackupEnvelopes(), password);
    }

    private DecodedBackup findNewestDecodableBackup(List<BackupFile> files, char[] password) throws Exception {
        Exception lastError = null;
        for (BackupFile file : files) {
            try {
                byte[] data = read(file);
                List<SmsMessage> messages = BackupCodec.decode(data, password);
                Log.i(TAG, "Fichier sélectionné pour restauration : " + file.name);
                return new DecodedBackup(messages);
            } catch (Exception invalid) {
                Log.w(TAG, "Sauvegarde invalide ignorée : " + file.name, invalid);
                lastError = invalid;
            }
        }
        if (lastError != null) throw lastError;
        return null;
    }

    private List<BackupFile> listValidBackupEnvelopes() throws Exception {
        List<BackupFile> valid = new ArrayList<>();
        for (BackupFile file : listBackupFiles()) {
            try {
                BackupCodec.validateEnvelope(read(file));
                valid.add(file);
            } catch (Exception invalid) {
                Log.w(TAG, "Fichier de sauvegarde invalide : " + file.name, invalid);
            }
        }
        return valid;
    }

    private List<BackupFile> listBackupFiles() throws Exception {
        Log.d(TAG, "Recherche sauvegarde : /" + RELATIVE_DIRECTORY);
        List<BackupFile> files = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? listMediaStoreBackups() : listLegacyBackups();
        Collections.sort(files, Comparator.comparingLong((BackupFile file) -> file.modified).reversed());
        for (BackupFile file : files) Log.d(TAG, "Fichier trouvé : " + file.name);
        return files;
    }

    private List<BackupFile> listMediaStoreBackups() {
        List<BackupFile> files = new ArrayList<>();
        String selection = MediaStore.Downloads.RELATIVE_PATH + "=? AND "
                + MediaStore.Downloads.DISPLAY_NAME + " LIKE ?";
        String[] arguments = {RELATIVE_DIRECTORY, "%" + BACKUP_EXTENSION};
        String[] projection = {MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.DATE_MODIFIED};
        try (Cursor cursor = context.getContentResolver().query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection, selection, arguments, null)) {
            if (cursor == null) return files;
            while (cursor.moveToNext()) {
                String name = cursor.getString(1);
                if (!isBackupFileName(name)) continue;
                Uri uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cursor.getLong(0));
                files.add(new BackupFile(name, cursor.getLong(2) * 1000L, uri, null));
            }
        }
        return files;
    }

    private List<BackupFile> listLegacyBackups() {
        List<BackupFile> files = new ArrayList<>();
        File directory = legacyDirectory();
        File[] entries = directory.listFiles(file -> file.isFile() && isBackupFileName(file.getName()));
        if (entries != null) for (File entry : entries)
            files.add(new BackupFile(entry.getName(), entry.lastModified(), null, entry));
        return files;
    }

    static boolean isBackupFileName(String name) {
        return name != null && name.toLowerCase(Locale.ROOT).endsWith(BACKUP_EXTENSION);
    }

    private byte[] read(BackupFile file) throws Exception {
        InputStream input = file.uri != null
                ? context.getContentResolver().openInputStream(file.uri) : new FileInputStream(file.file);
        if (input == null) throw new java.io.IOException("Impossible d'ouvrir " + file.name);
        try (InputStream source = input; ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = source.read(buffer)) != -1) bytes.write(buffer, 0, count);
            return bytes.toByteArray();
        }
    }

    private void delete(BackupFile file) throws Exception {
        boolean deleted = file.uri != null
                ? context.getContentResolver().delete(file.uri, null, null) > 0
                : file.file.delete();
        if (!deleted) throw new java.io.IOException("Impossible de supprimer l'ancienne sauvegarde " + file.name);
    }

    @SuppressWarnings("deprecation")
    private File legacyDirectory() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DIRECTORY_NAME);
    }

    private File legacyFile(String name) { return new File(legacyDirectory(), name); }

    private static final class BackupFile {
        final String name;
        final long modified;
        final Uri uri;
        final File file;

        BackupFile(String name, long modified, Uri uri, File file) {
            this.name = name;
            this.modified = modified;
            this.uri = uri;
            this.file = file;
        }
    }

    private static final class DecodedBackup {
        final List<SmsMessage> messages;
        DecodedBackup(List<SmsMessage> messages) { this.messages = messages; }
    }
}
