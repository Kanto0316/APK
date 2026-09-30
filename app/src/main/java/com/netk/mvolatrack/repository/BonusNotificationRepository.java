package com.netk.mvolatrack.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.netk.mvolatrack.database.AppDatabase;
import com.netk.mvolatrack.database.NotificationBonus;
import com.netk.mvolatrack.database.NotificationBonusDao;
import com.netk.mvolatrack.history.HistoryTransaction;
import com.netk.mvolatrack.notification.BonusNotificationFactory;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BonusNotificationRepository {
    private static final String TAG = "BonusNotification";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private final NotificationBonusDao dao;
    public BonusNotificationRepository(Context context) {
        NotificationBonusDao availableDao = null;
        try {
            AppDatabase database = AppDatabase.getInstance(context);
            // Room opens lazily. Force schema validation/migration while the Activity can
            // still fall back to its empty state instead of failing on a worker thread.
            database.getOpenHelper().getWritableDatabase();
            availableDao = database.notificationBonusDao();
        } catch (Throwable error) {
            logError("initialization", error);
        }
        dao = availableDao;
    }
    public LiveData<List<NotificationBonus>> observeAll() {
        if (dao == null) return new MutableLiveData<>(Collections.emptyList());
        try {
            LiveData<List<NotificationBonus>> values = dao.observeAll();
            return values == null
                    ? new MutableLiveData<>(Collections.emptyList()) : values;
        } catch (Throwable error) {
            logError("observeAll", error);
            return new MutableLiveData<>(Collections.emptyList());
        }
    }
    public LiveData<Integer> observeUnreadCount() {
        if (dao == null) return new MutableLiveData<>(0);
        try {
            LiveData<Integer> count = dao.observeUnreadCount();
            return count == null ? new MutableLiveData<>(0) : count;
        } catch (Throwable error) {
            logError("observeUnreadCount", error);
            return new MutableLiveData<>(0);
        }
    }
    public void synchronize(List<HistoryTransaction> transactions) {
        EXECUTOR.execute(() -> {
            long now = System.currentTimeMillis();
            if (dao == null || transactions == null) return;
            for (HistoryTransaction transaction : transactions) {
                NotificationBonus notification = BonusNotificationFactory.from(transaction, now);
                if (notification != null) {
                    try {
                        dao.insert(notification);
                    } catch (Throwable error) {
                        logError("insert", error);
                    }
                }
            }
        });
    }
    public void markAllRead() {
        if (dao == null) return;
        EXECUTOR.execute(() -> {
            try {
                dao.markAllRead();
            } catch (Throwable error) {
                logError("markAllRead", error);
            }
        });
    }

    private static void logError(String operation, Throwable error) {
        String type = error == null ? "unknown" : error.getClass().getName();
        String message = error == null || error.getMessage() == null
                ? "no message" : error.getMessage();
        Log.e(TAG, operation + " failed (" + type + "): " + message, error);
    }
}
