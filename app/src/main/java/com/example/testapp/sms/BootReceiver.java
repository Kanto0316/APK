package com.example.testapp.sms;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

/** Re-enables the manifest SMS receiver after a completed device boot. */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            return;
        }

        ComponentName smsReceiver = new ComponentName(context, SmsReceiver.class);
        context.getPackageManager().setComponentEnabledSetting(smsReceiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        boolean granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
        Log.i(TAG, "Récepteur SMS réactivé après " + action
                + (granted ? "; permission accordée" : "; permission SMS absente"));
    }
}
