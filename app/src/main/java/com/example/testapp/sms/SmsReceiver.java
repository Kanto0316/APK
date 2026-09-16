package com.example.testapp.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import com.example.testapp.database.Transaction;
import com.example.testapp.notification.NotificationHelper;
import com.example.testapp.repository.TransactionRepository;

public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages == null || messages.length == 0) return;

        StringBuilder body = new StringBuilder();
        for (SmsMessage message : messages) body.append(message.getMessageBody());
        SmsMessage first = messages[0];
        Transaction transaction = new SmsParser().parse(body.toString(),
                first.getDisplayOriginatingAddress(), first.getTimestampMillis());
        if (transaction == null) return;

        PendingResult pendingResult = goAsync();
        Context appContext = context.getApplicationContext();
        new TransactionRepository(appContext).insert(transaction, () -> {
            new NotificationHelper(appContext).showTransactionSaved(transaction);
            pendingResult.finish();
        });
    }
}
