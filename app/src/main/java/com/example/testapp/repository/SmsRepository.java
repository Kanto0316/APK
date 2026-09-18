package com.example.testapp.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.testapp.database.AppDatabase;
import com.example.testapp.database.SmsDao;
import com.example.testapp.database.SmsMessage;
import com.example.testapp.backup.SmsBackupManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Single local-only data source for SMS history. */
public class SmsRepository {
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
        insert(message, error -> {
            if (onComplete != null) onComplete.run();
        });
    }

    /** Callback used by short-lived components such as broadcast receivers. */
    public interface InsertCallback {
        void onComplete(RuntimeException error);
    }

    public void insert(SmsMessage message, InsertCallback callback) {
        DATABASE_EXECUTOR.execute(() -> {
            RuntimeException error = null;
            try {
                dao.insert(message);
                new SmsBackupManager(appContext).automaticBackup();
            } catch (RuntimeException failure) {
                error = failure;
            } finally {
                if (callback != null) callback.onComplete(error);
            }
        });
    }

}
