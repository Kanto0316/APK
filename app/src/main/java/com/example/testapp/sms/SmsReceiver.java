package com.example.testapp.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import com.example.testapp.repository.SmsRepository;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            Log.w(TAG, "Diffusion ignorée : action SMS_RECEIVED absente");
            return;
        }
        Log.i(TAG, "Diffusion SMS_RECEIVED reçue en arrière-plan");
        SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (parts == null || parts.length == 0) {
            Log.w(TAG, "SMS_RECEIVED sans segment décodable");
            return;
        }

        StringBuilder completeBody = new StringBuilder();
        for (SmsMessage part : parts) {
            if (part != null && part.getMessageBody() != null) {
                completeBody.append(part.getMessageBody());
            }
        }
        SmsMessage first = parts[0];
        if (first == null) {
            Log.w(TAG, "Premier segment SMS nul, réception ignorée");
            return;
        }

        PendingResult pendingResult = goAsync();
        com.example.testapp.database.SmsMessage localMessage =
                com.example.testapp.database.SmsMessage.create(
                        first.getDisplayOriginatingAddress(), completeBody.toString(),
                        first.getTimestampMillis(), false);
        try {
            new SmsRepository(context).insert(localMessage, error -> {
                try {
                    if (error == null) {
                        Log.i(TAG, "SMS enregistré dans Room (" + parts.length + " segment(s))");
                    } else {
                        Log.e(TAG, "Échec de l'insertion du SMS dans Room", error);
                    }
                } finally {
                    pendingResult.finish();
                }
            });
        } catch (RuntimeException schedulingFailure) {
            Log.e(TAG, "Impossible de planifier l'insertion Room", schedulingFailure);
            pendingResult.finish();
        }
    }
}
