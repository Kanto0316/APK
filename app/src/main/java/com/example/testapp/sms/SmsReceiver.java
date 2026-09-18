package com.example.testapp.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import com.example.testapp.repository.SmsRepository;

public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
        SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (parts == null || parts.length == 0) return;

        StringBuilder completeBody = new StringBuilder();
        for (SmsMessage part : parts) {
            if (part != null && part.getMessageBody() != null) {
                completeBody.append(part.getMessageBody());
            }
        }
        SmsMessage first = parts[0];
        if (first == null) return;

        PendingResult pendingResult = goAsync();
        com.example.testapp.database.SmsMessage localMessage =
                com.example.testapp.database.SmsMessage.create(
                        first.getDisplayOriginatingAddress(), completeBody.toString(),
                        first.getTimestampMillis(), false);
        new SmsRepository(context).insert(localMessage, pendingResult::finish);
    }
}
