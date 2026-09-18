package com.example.testapp.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.Telephony;

import androidx.lifecycle.LiveData;

import com.example.testapp.database.AppDatabase;
import com.example.testapp.database.SmsDao;
import com.example.testapp.database.SmsMessage;
import com.example.testapp.backup.SmsBackupManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Single local-only data source for SMS history. */
public class SmsRepository {
    private static final String PREFERENCES = "sms_import";
    private static final String INITIAL_IMPORT_DONE = "initial_import_done";
    private static final ExecutorService DATABASE_EXECUTOR = Executors.newSingleThreadExecutor();

    private final Context appContext;
    private final SmsDao dao;

    public SmsRepository(Context context) {
        appContext = context.getApplicationContext();
        dao = AppDatabase.getInstance(appContext).smsDao();
    }

    public LiveData<List<SmsMessage>> observeMessages() {
        return dao.observeAllNewestFirst();
    }

    public void insert(SmsMessage message) {
        DATABASE_EXECUTOR.execute(() -> {
            dao.insert(message);
            new SmsBackupManager(appContext).automaticBackup();
        });
    }

    public void insert(SmsMessage message, Runnable onComplete) {
        DATABASE_EXECUTOR.execute(() -> {
            try {
                dao.insert(message);
                new SmsBackupManager(appContext).automaticBackup();
            } finally {
                if (onComplete != null) onComplete.run();
            }
        });
    }

    /** Imports the Android inbox once; the unique Room index also makes retries safe. */
    public void importInboxOnce(Runnable onComplete, Runnable onPermissionDenied) {
        SharedPreferences preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        if (preferences.getBoolean(INITIAL_IMPORT_DONE, false)) {
            if (onComplete != null) onComplete.run();
            return;
        }
        DATABASE_EXECUTOR.execute(() -> {
            List<SmsMessage> messages = new ArrayList<>();
            String[] projection = {Telephony.Sms.ADDRESS, Telephony.Sms.BODY,
                    Telephony.Sms.DATE, Telephony.Sms.READ};
            try (Cursor cursor = appContext.getContentResolver().query(
                    Telephony.Sms.Inbox.CONTENT_URI, projection, null, null, null)) {
                if (cursor != null) {
                    int sender = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS);
                    int body = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY);
                    int date = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE);
                    int read = cursor.getColumnIndexOrThrow(Telephony.Sms.READ);
                    while (cursor.moveToNext()) {
                        messages.add(SmsMessage.create(cursor.getString(sender), cursor.getString(body),
                                cursor.getLong(date), cursor.getInt(read) != 0));
                    }
                }
                dao.insertAll(messages);
                preferences.edit().putBoolean(INITIAL_IMPORT_DONE, true).apply();
                new SmsBackupManager(appContext).automaticBackup();
                if (onComplete != null) onComplete.run();
            } catch (SecurityException denied) {
                if (onPermissionDenied != null) onPermissionDenied.run();
            }
        });
    }
}
