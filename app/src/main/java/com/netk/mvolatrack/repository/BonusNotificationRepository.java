package com.netk.mvolatrack.repository;

import android.content.Context;
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
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private final NotificationBonusDao dao;
    public BonusNotificationRepository(Context context) {
        NotificationBonusDao availableDao = null;
        try {
            availableDao = AppDatabase.getInstance(context).notificationBonusDao();
        } catch (RuntimeException ignored) {
            // Consumers receive empty LiveData when Room cannot be initialized.
        }
        dao = availableDao;
    }
    public LiveData<List<NotificationBonus>> observeAll() {
        if (dao == null) return new MutableLiveData<>(Collections.emptyList());
        try {
            LiveData<List<NotificationBonus>> values = dao.observeAll();
            return values == null
                    ? new MutableLiveData<>(Collections.emptyList()) : values;
        } catch (RuntimeException ignored) {
            return new MutableLiveData<>(Collections.emptyList());
        }
    }
    public LiveData<Integer> observeUnreadCount() {
        if (dao == null) return new MutableLiveData<>(0);
        try {
            LiveData<Integer> count = dao.observeUnreadCount();
            return count == null ? new MutableLiveData<>(0) : count;
        } catch (RuntimeException ignored) {
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
                    } catch (RuntimeException ignored) {
                        // Keep processing possible later updates even if persistence is unavailable.
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
            } catch (RuntimeException ignored) {
                // Reading the inbox should remain usable if this best-effort update fails.
            }
        });
    }
}
