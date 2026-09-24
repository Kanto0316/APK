package com.example.testapp.overlay;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.example.testapp.R;
import com.example.testapp.sms.MvolaMessageParser;

/** Owns the single WindowManager view used while the application is in the background. */
final class SystemTransactionOverlay {
    private final Context context;
    private final WindowManager windowManager;
    private View attachedView;

    SystemTransactionOverlay(Context context) {
        this.context = context.getApplicationContext();
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    synchronized boolean show(String sender, MvolaMessageParser.ParsedTransaction transaction,
                              Runnable onAcknowledged) {
        remove();
        if (windowManager == null) return false;
        View view = LayoutInflater.from(context).inflate(R.layout.transaction_overlay, null);
        TransactionCardBinder.bind(view, sender, transaction, ignored -> {
            remove();
            onAcknowledged.run();
        });
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                cardWidth(), WindowManager.LayoutParams.WRAP_CONTENT, type,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        try {
            windowManager.addView(view, params);
            attachedView = view;
            return true;
        } catch (RuntimeException ignored) {
            attachedView = null;
            return false;
        }
    }

    synchronized void remove() {
        if (attachedView == null || windowManager == null) return;
        View oldView = attachedView;
        attachedView = null;
        try {
            windowManager.removeViewImmediate(oldView);
        } catch (IllegalArgumentException ignored) {
            // Already detached by Android; references are still released.
        }
    }

    private int cardWidth() {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.min((int) (360 * density),
                context.getResources().getDisplayMetrics().widthPixels - (int) (32 * density));
    }
}
