package com.netk.mvolatrack.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.netk.mvolatrack.database.AppDatabase;
import com.netk.mvolatrack.database.SmsDao;
import com.netk.mvolatrack.database.SmsMessage;
import com.netk.mvolatrack.backup.SmsBackupManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Single local-only data source for SMS history. */
public class SmsRepository {
    private static final String TAG = "SmsRepository";
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

    public interface MessagesCallback {
        void onLoaded(List<SmsMessage> messages);
    }

    public interface MessageCallback {
        void onLoaded(SmsMessage message);
    }

    public void loadMessage(long id, MessageCallback callback) {
        DATABASE_EXECUTOR.execute(() -> callback.onLoaded(dao.getById(id)));
    }

    public void loadMessages(MessagesCallback callback) {
        DATABASE_EXECUTOR.execute(() -> callback.onLoaded(dao.getAllNewestFirst()));
    }

    public void insert(SmsMessage message) {
        DATABASE_EXECUTOR.execute(() -> {
            dao.insert(message);
            new SmsBackupManager(appContext).automaticBackup();
        });
    }

    public void insert(SmsMessage message, Runnable onComplete) {
        insert(message, (rowId, error) -> {
            if (onComplete != null) onComplete.run();
        });
    }

    /** Callback used by short-lived components such as broadcast receivers. */
    public interface InsertCallback {
        void onComplete(long rowId, RuntimeException error);
    }

    public void insert(SmsMessage message, InsertCallback callback) {
        DATABASE_EXECUTOR.execute(() -> {
            RuntimeException error = null;
            long rowId = -1L;
            try {
                rowId = dao.insert(message);
            } catch (RuntimeException failure) {
                error = failure;
            } finally {
                if (callback != null) callback.onComplete(rowId, error);
            }
            if (error == null) {
                try {
                    new SmsBackupManager(appContext).automaticBackup();
                } catch (RuntimeException backupFailure) {
                    // The Room transaction already succeeded; a backup issue must not report it as failed.
                    Log.e(TAG, "Insertion Room réussie, mais sauvegarde automatique impossible",
                            backupFailure);
                }
            }
        });
    }

}
