package com.netk.mvolatrack.overlay;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;

import com.netk.mvolatrack.notification.NotificationHelper;
import com.netk.mvolatrack.repository.SmsRepository;
import com.netk.mvolatrack.sms.MvolaMessageParser;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicLong;

/** Global owner of notification, modal state and the transaction FIFO. */
public final class TransactionOverlayCoordinator {
    private static final String STATES = "transaction_presentation_states";
    private static volatile TransactionOverlayCoordinator instance;
    private static final AtomicLong WARNING_IDS = new AtomicLong(-1L);

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final TransactionDialog dialog = new TransactionDialog();
    private final SystemTransactionOverlay systemOverlay;
    private final TransactionPresentationQueue<Item> queue = new TransactionPresentationQueue<>();
    private final SharedPreferences states;
    private WeakReference<AppCompatActivity> visibleActivity = new WeakReference<>(null);
    private volatile long renderedId;

    private TransactionOverlayCoordinator(Context context) {
        this.context = context.getApplicationContext();
        systemOverlay = new SystemTransactionOverlay(this.context);
        states = this.context.getSharedPreferences(STATES, Context.MODE_PRIVATE);
    }

    public static TransactionOverlayCoordinator get(Context context) {
        if (instance == null) synchronized (TransactionOverlayCoordinator.class) {
            if (instance == null) instance = new TransactionOverlayCoordinator(context);
        }
        return instance;
    }

    public void attach(AppCompatActivity activity) {
        mainHandler.post(() -> {
            visibleActivity = new WeakReference<>(activity);
            systemOverlay.remove();
            renderedId = 0;
            presentCurrent();
        });
    }

    public void detach(AppCompatActivity activity) {
        mainHandler.post(() -> {
            if (visibleActivity.get() == activity) visibleActivity.clear();
            dialog.dismissSilently();
            renderedId = 0;
            presentCurrent();
        });
    }

    /** Called exactly once, only after the SMS row has been inserted successfully. */
    public void onTransactionReceived(long smsId, String sender,
                                      MvolaMessageParser.ParsedTransaction transaction) {
        mainHandler.post(() -> {
            Item item = new Item(smsId, sender, transaction);
            if (isAcknowledged(smsId)) return;
            if (!states.getBoolean("notified_" + smsId, false)) {
                new NotificationHelper(context).showTransaction(smsId, sender, transaction);
                states.edit().putBoolean("notified_" + smsId, true).apply();
            }
            queue.offer(item, smsId);
            presentCurrent();
        });
    }

    /** Presents a warning without persisting either the SMS or presentation state. */
    public void onUnauthorizedSender(String sender) {
        mainHandler.post(() -> {
            long id = WARNING_IDS.getAndDecrement();
            new NotificationHelper(context).showSpamWarning(sender);
            queue.offer(Item.spamWarning(id, sender), id);
            presentCurrent();
        });
    }

    /** Resolves a notification's database ID instead of trusting display fields in its Intent. */
    public void openFromNotification(long smsId) {
        if (smsId <= 0 || isAcknowledged(smsId)) return;
        new SmsRepository(context).loadMessage(smsId, message -> {
            if (message == null) return;
            if (!com.netk.mvolatrack.sms.AuthorizedSmsSenders.isAuthorized(message.sender)) return;
            MvolaMessageParser.ParsedTransaction parsed =
                    MvolaMessageParser.parse(message.messageBody, message.receivedDate);
            if (parsed != null) mainHandler.post(() -> {
                queue.offer(new Item(message.id, message.sender, parsed), message.id);
                presentCurrent();
            });
        });
    }

    public boolean isTransactionModalShowing() {
        return renderedId != 0;
    }

    private void presentCurrent() {
        Item item = queue.acquireNext();
        if (item == null) return;
        if (renderedId == item.id) return;
        AppCompatActivity activity = visibleActivity.get();
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            systemOverlay.remove();
            if (item.spamWarning) {
                dialog.showSpamWarning(activity, item.sender, () -> acknowledge(item.id));
            } else {
                dialog.show(activity, item.sender, item.transaction, () -> acknowledge(item.id));
            }
            renderedId = item.id;
            return;
        }
        dialog.dismissSilently();
        boolean canOverlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || Settings.canDrawOverlays(context);
        // Also attempt while keyguard is active. Android/OEM window policy remains authoritative;
        // the already-posted notification is the fallback if addView is rejected or hidden.
        boolean shown = item.spamWarning
                ? canOverlay && systemOverlay.showSpamWarning(item.sender,
                        () -> acknowledge(item.id))
                : canOverlay && systemOverlay.show(item.sender, item.transaction,
                        () -> acknowledge(item.id));
        if (shown) renderedId = item.id;
    }

    private void acknowledge(long id) {
        if (id > 0) states.edit().putBoolean("acknowledged_" + id, true).apply();
        dialog.dismissSilently();
        systemOverlay.remove();
        renderedId = 0;
        queue.acknowledge(id);
        presentCurrent();
    }

    private boolean isAcknowledged(long id) {
        return states.getBoolean("acknowledged_" + id, false);
    }

    private static final class Item implements TransactionPresentationQueue.Identified {
        final long id;
        final String sender;
        final MvolaMessageParser.ParsedTransaction transaction;
        final boolean spamWarning;
        Item(long id, String sender, MvolaMessageParser.ParsedTransaction transaction) {
            this(id, sender, transaction, false);
        }
        private Item(long id, String sender, MvolaMessageParser.ParsedTransaction transaction,
                     boolean spamWarning) {
            this.id = id; this.sender = sender; this.transaction = transaction;
            this.spamWarning = spamWarning;
        }
        static Item spamWarning(long id, String sender) {
            return new Item(id, sender, null, true);
        }
        @Override public long id() { return id; }
    }
}
