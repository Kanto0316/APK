package com.example.testapp.overlay;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;

import com.example.testapp.notification.NotificationHelper;
import com.example.testapp.sms.MvolaMessageParser;

import java.lang.ref.WeakReference;

/** Routes one already-parsed transaction to the appropriate Android presentation surface. */
public final class TransactionOverlayCoordinator {
    private static volatile TransactionOverlayCoordinator instance;

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final TransactionDialog dialog = new TransactionDialog();
    private final SystemTransactionOverlay systemOverlay;
    private WeakReference<AppCompatActivity> visibleActivity = new WeakReference<>(null);

    private TransactionOverlayCoordinator(Context context) {
        this.context = context.getApplicationContext();
        systemOverlay = new SystemTransactionOverlay(this.context);
    }

    public static TransactionOverlayCoordinator get(Context context) {
        if (instance == null) {
            synchronized (TransactionOverlayCoordinator.class) {
                if (instance == null) instance = new TransactionOverlayCoordinator(context);
            }
        }
        return instance;
    }

    public void attach(AppCompatActivity activity) {
        mainHandler.post(() -> {
            visibleActivity = new WeakReference<>(activity);
            systemOverlay.remove();
        });
    }

    public void detach(AppCompatActivity activity) {
        mainHandler.post(() -> {
            if (visibleActivity.get() == activity) visibleActivity.clear();
            dialog.dismiss();
        });
    }

    public void show(String sender, MvolaMessageParser.ParsedTransaction transaction) {
        mainHandler.post(() -> route(sender, transaction));
    }

    private void route(String sender, MvolaMessageParser.ParsedTransaction transaction) {
        AppCompatActivity activity = visibleActivity.get();
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            systemOverlay.remove();
            dialog.show(activity, sender, transaction);
            return;
        }

        dialog.dismiss();
        KeyguardManager keyguard =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        boolean deviceLocked = keyguard != null && keyguard.isKeyguardLocked();
        boolean canOverlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || Settings.canDrawOverlays(context);
        if (!deviceLocked && canOverlay) {
            if (!systemOverlay.show(sender, transaction)) {
                new NotificationHelper(context).showParsedTransaction(sender, transaction);
            }
        } else {
            systemOverlay.remove();
            new NotificationHelper(context).showParsedTransaction(sender, transaction);
        }
    }
}
