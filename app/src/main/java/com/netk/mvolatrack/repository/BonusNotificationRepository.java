package com.netk.mvolatrack.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.netk.mvolatrack.database.AppDatabase;
import com.netk.mvolatrack.database.NotificationBonus;
import com.netk.mvolatrack.database.NotificationBonusDao;
import com.netk.mvolatrack.database.NotificationType;
import com.netk.mvolatrack.activation.ActivationResponse;
import com.netk.mvolatrack.activation.ActivationStore;
import com.netk.mvolatrack.activation.ActivationVerifier;
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
        synchronizeActiveLicense(context.getApplicationContext());
    }

    /** Adds one stable inbox entry for the currently verified licence. */
    private void synchronizeActiveLicense(Context context) {
        if (dao == null) return;
        EXECUTOR.execute(() -> {
            try {
                ActivationVerifier.Verification verification = ActivationStore.status(context);
                if (verification.result != ActivationVerifier.Result.VALID
                        || verification.license == null) return;
                ActivationResponse licence = verification.license;
                long now = System.currentTimeMillis();
                long activatedAt = licence.getIssuedAt() > 0
                        ? licence.getIssuedAt() * 1000L : now;
                Long expiresAt = licence.getExpiresAt() == null ? null
                        : licence.getExpiresAt() * 1000L;
                long remainingDays = expiresAt == null ? -1
                        : Math.max(0, (expiresAt - now + 86_399_999L) / 86_400_000L);
                String kind = licence.getType() == ActivationResponse.LicenseType.WEEK
                        ? "Premium – Hebdomadaire"
                        : licence.getType() == ActivationResponse.LicenseType.MONTH
                        ? "Premium – Mensuelle" : "Premium – Permanente";
                String metadata = "Type de licence : " + kind + "\n"
                        + "Date d’activation : " + activatedAt + "\n"
                        + "Date d’expiration : " + (expiresAt == null ? "Permanente" : expiresAt) + "\n"
                        + "Jours restants : " + (remainingDays < 0 ? "Illimités" : remainingDays + " jours");
                String proof = ActivationStore.loadProof(context);
                String key = "licence-active-" + Integer.toHexString(
                        proof == null ? 0 : proof.hashCode());
                dao.insertAndTrim(NotificationBonus.general(now, key, NotificationType.LICENCE,
                        "Licence active", "Votre licence MVolaCash est active.", metadata));
            } catch (Throwable error) {
                logError("synchronizeActiveLicense", error);
            }
        });
    }

    /** Persists a non-bonus event (licence lifecycle, update, backup or application info). */
    public void addNotification(NotificationType type, String uniqueKey, String title,
            String message, String metadata) {
        if (dao == null || type == null || uniqueKey == null || title == null || message == null) return;
        EXECUTOR.execute(() -> {
            try {
                dao.insertAndTrim(NotificationBonus.general(System.currentTimeMillis(), uniqueKey,
                        type, title, message, metadata));
            } catch (Throwable error) {
                logError("addNotification", error);
            }
        });
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
                        dao.insertAndTrim(notification);
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

    public void markRead(long notificationId) {
        if (dao == null || notificationId <= 0) return;
        EXECUTOR.execute(() -> {
            try {
                dao.markRead(notificationId);
            } catch (Throwable error) {
                logError("markRead", error);
            }
        });
    }

    /** Deletes only the explicitly selected inbox entries, without changing read state. */
    public void deleteNotifications(List<Long> notificationIds, Runnable onComplete) {
        if (dao == null || notificationIds == null || notificationIds.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }
        List<Long> ids = new java.util.ArrayList<>(notificationIds);
        EXECUTOR.execute(() -> {
            try {
                dao.deleteByIds(ids);
            } catch (Throwable error) {
                logError("deleteNotifications", error);
            } finally {
                if (onComplete != null) {
                    new Handler(Looper.getMainLooper()).post(onComplete);
                }
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
