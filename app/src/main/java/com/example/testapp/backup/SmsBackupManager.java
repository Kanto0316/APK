package com.example.testapp.backup;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.example.testapp.database.AppDatabase;
import com.example.testapp.database.SmsMessage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Coordinates encrypted Room exports in shared storage, which survives app removal. */
public final class SmsBackupManager {
    public static final String FILE_NAME = "sms-history.smsbackup";
    private static final String RELATIVE_DIRECTORY = Environment.DIRECTORY_DOWNLOADS + "/SmsTracker/";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private final Context context;
    private final PasswordVault vault;

    public SmsBackupManager(Context context) {
        this.context = context.getApplicationContext();
        vault = new PasswordVault(this.context);
    }

    public interface Callback<T> { void complete(T result, Exception error); }

    public void backup(char[] password, boolean rememberForAutomaticBackup, Callback<Date> callback) {
        char[] passwordCopy = Arrays.copyOf(password, password.length);
        EXECUTOR.execute(() -> {
            try {
                List<SmsMessage> messages = AppDatabase.getInstance(context).smsDao().getAllNewestFirst();
                byte[] encoded = BackupCodec.encode(messages, passwordCopy);
                writeBackup(encoded);
                if (rememberForAutomaticBackup) vault.save(passwordCopy);
                callback.complete(getLastBackupDateNow(), null);
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
                byte[] data = readBackup();
                if (data == null) throw new BackupCodec.BackupException("Aucune sauvegarde trouvée");
                List<SmsMessage> messages = BackupCodec.decode(data, passwordCopy);
                AppDatabase.getInstance(context).smsDao().insertAll(messages);
                vault.save(passwordCopy);
                callback.complete(messages.size(), null);
            } catch (Exception error) { callback.complete(null, error); }
            finally { Arrays.fill(passwordCopy, '\0'); }
        });
    }

    public void findBackup(Callback<Date> callback) {
        EXECUTOR.execute(() -> {
            try { callback.complete(getLastBackupDate(), null); }
            catch (Exception error) { callback.complete(null, error); }
        });
    }

    private void writeBackup(byte[] data) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            Uri existing = findMediaStoreUri();
            Uri uri = existing;
            if (uri == null) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream");
                values.put(MediaStore.Downloads.RELATIVE_PATH, RELATIVE_DIRECTORY);
                values.put(MediaStore.Downloads.IS_PENDING, 1);
                uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new java.io.IOException("Impossible de créer le fichier de sauvegarde");
            }
            try (OutputStream output = resolver.openOutputStream(uri, "wt")) {
                if (output == null) throw new java.io.IOException("Impossible d'ouvrir la sauvegarde");
                output.write(data);
            }
            ContentValues ready = new ContentValues();
            ready.put(MediaStore.Downloads.IS_PENDING, 0);
            ready.put(MediaStore.Downloads.DATE_MODIFIED, System.currentTimeMillis() / 1000L);
            resolver.update(uri, ready, null, null);
        } else {
            File target = legacyFile();
            File parent = target.getParentFile();
            if (parent == null || (!parent.exists() && !parent.mkdirs())) throw new java.io.IOException("Dossier de sauvegarde inaccessible");
            File temporary = new File(parent, FILE_NAME + ".tmp");
            try (FileOutputStream output = new FileOutputStream(temporary)) { output.write(data); output.getFD().sync(); }
            if (target.exists() && !target.delete()) throw new java.io.IOException("Ancienne sauvegarde inaccessible");
            if (!temporary.renameTo(target)) throw new java.io.IOException("Impossible de finaliser la sauvegarde");
        }
    }

    private byte[] readBackup() throws Exception {
        InputStream input;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Uri uri = findMediaStoreUri();
            if (uri == null) return null;
            input = context.getContentResolver().openInputStream(uri);
        } else {
            File file = legacyFile();
            if (!file.exists()) return null;
            input = new FileInputStream(file);
        }
        if (input == null) return null;
        try (InputStream source = input; ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int read;
            while ((read = source.read(buffer)) != -1) bytes.write(buffer, 0, read);
            return bytes.toByteArray();
        }
    }

    private Date getLastBackupDate() throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Uri uri = findMediaStoreUri();
            if (uri == null) return null;
            try (Cursor cursor = context.getContentResolver().query(uri,
                    new String[]{MediaStore.Downloads.DATE_MODIFIED}, null, null, null)) {
                return cursor != null && cursor.moveToFirst() ? new Date(cursor.getLong(0) * 1000L) : null;
            }
        }
        File file = legacyFile();
        return file.exists() ? new Date(file.lastModified()) : null;
    }

    private Date getLastBackupDateNow() { return new Date(); }

    private Uri findMediaStoreUri() {
        String selection = MediaStore.Downloads.DISPLAY_NAME + "=? AND " + MediaStore.Downloads.RELATIVE_PATH + "=?";
        try (Cursor cursor = context.getContentResolver().query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Downloads._ID}, selection,
                new String[]{FILE_NAME, RELATIVE_DIRECTORY}, MediaStore.Downloads.DATE_MODIFIED + " DESC")) {
            if (cursor != null && cursor.moveToFirst()) {
                return Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, Long.toString(cursor.getLong(0)));
            }
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private File legacyFile() {
        return new File(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "SmsTracker"), FILE_NAME);
    }
}
