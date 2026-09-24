package com.example.testapp.overlay;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.testapp.R;
import com.example.testapp.sms.MvolaMessageParser;

/** Normal application window used whenever Suivi SMS is visible. */
public final class TransactionDialog {
    private Dialog dialog;

    public void show(AppCompatActivity activity, String sender,
                     MvolaMessageParser.ParsedTransaction transaction, Runnable onAcknowledged) {
        dismissSilently();
        if (activity.isFinishing() || activity.isDestroyed()) return;
        View content = LayoutInflater.from(activity).inflate(R.layout.transaction_overlay, null);
        dialog = new Dialog(activity);
        dialog.setContentView(content);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        TransactionCardBinder.bind(content, sender, transaction, view -> {
            dismissSilently();
            onAcknowledged.run();
        });
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
        if (window != null) window.setLayout(dp(activity, 360),
                WindowManager.LayoutParams.WRAP_CONTENT);
    }

    public void dismissSilently() {
        if (dialog != null) {
            if (dialog.isShowing()) dialog.dismiss();
            dialog = null;
        }
    }

    private static int dp(AppCompatActivity activity, int value) {
        float density = activity.getResources().getDisplayMetrics().density;
        int desired = (int) (value * density);
        return Math.min(desired, activity.getResources().getDisplayMetrics().widthPixels
                - (int) (32 * density));
    }
}
