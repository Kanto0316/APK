package com.example.testapp.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;

public class SmsReceiver extends BroadcastReceiver {
    public static final String ACTION_SMS_INBOX_CHANGED =
            "com.example.testapp.action.SMS_INBOX_CHANGED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
        Intent refreshIntent = new Intent(ACTION_SMS_INBOX_CHANGED)
                .setPackage(context.getPackageName());
        context.sendBroadcast(refreshIntent);
    }
}
