package com.netk.mvolatrack.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.netk.mvolatrack.database.AppDatabase;
import com.netk.mvolatrack.database.NotificationBonus;
import com.netk.mvolatrack.database.NotificationBonusDao;
import com.netk.mvolatrack.history.HistoryTransaction;
import com.netk.mvolatrack.notification.BonusNotificationFactory;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BonusNotificationRepository {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private final NotificationBonusDao dao;
    public BonusNotificationRepository(Context context) {
        dao = AppDatabase.getInstance(context).notificationBonusDao();
    }
    public LiveData<List<NotificationBonus>> observeAll() { return dao.observeAll(); }
    public LiveData<Integer> observeUnreadCount() { return dao.observeUnreadCount(); }
    public void synchronize(List<HistoryTransaction> transactions) {
        EXECUTOR.execute(() -> {
            long now = System.currentTimeMillis();
            if (transactions == null) return;
            for (HistoryTransaction transaction : transactions) {
                NotificationBonus notification = BonusNotificationFactory.from(transaction, now);
                if (notification != null) dao.insert(notification);
            }
        });
    }
    public void markAllRead() { EXECUTOR.execute(dao::markAllRead); }
}
