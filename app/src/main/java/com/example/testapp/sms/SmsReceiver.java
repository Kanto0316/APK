package com.example.testapp.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import com.example.testapp.repository.SmsRepository;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        Log.i(TAG, "onReceive() appelé ; action=" + action);
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(action)) {
            Log.w(TAG, "Diffusion ignorée : action reçue différente de SMS_RECEIVED");
            return;
        }
        SmsReceptionDiagnostics.recordReceiverInvocation(context, System.currentTimeMillis());

        Bundle extras = intent.getExtras();
        Object[] rawPdus = extras == null ? null : (Object[]) extras.get("pdus");
        int bundlePartCount = rawPdus == null ? 0 : rawPdus.length;
        Log.i(TAG, "Action SMS_RECEIVED confirmée ; " + bundlePartCount
                + " segment(s) présent(s) dans le bundle");

        SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (parts == null || parts.length == 0) {
            Log.w(TAG, "SMS_RECEIVED sans segment décodable");
            return;
        }

        StringBuilder completeBody = new StringBuilder();
        SmsMessage first = null;
        for (SmsMessage part : parts) {
            if (part != null) {
                if (first == null) first = part;
            }
            if (part != null && part.getMessageBody() != null) {
                completeBody.append(part.getMessageBody());
            }
        }
        if (first == null) {
            Log.w(TAG, "Tous les segments SMS sont nuls, réception ignorée");
            return;
        }

        String sender = first.getDisplayOriginatingAddress();
        long receivedAt = first.getTimestampMillis();
        String body = completeBody.toString();
        Log.d(TAG, "SMS extrait ; expéditeur=" + sender + ", date=" + receivedAt
                + ", contenu=" + body);

        PendingResult pendingResult = goAsync();
        com.example.testapp.database.SmsMessage localMessage =
                com.example.testapp.database.SmsMessage.create(
                        sender, body, receivedAt, false);
        try {
            Context appContext = context.getApplicationContext();
            Log.i(TAG, "Planification de l'insertion Room ; clé=" + localMessage.uniqueKey);
            new SmsRepository(appContext).insert(localMessage, (rowId, error) -> {
                try {
                    if (error == null) {
                        if (rowId == -1L) {
                            Log.w(TAG, "Insertion Room ignorée car ce SMS existe déjà ; clé="
                                    + localMessage.uniqueKey);
                        } else {
                            SmsReceptionDiagnostics.recordRoomInsertion(
                                    appContext, System.currentTimeMillis());
                            Log.i(TAG, "Insertion Room confirmée ; rowId=" + rowId + " ("
                                    + parts.length + " segment(s))");
                        }
                    } else {
                        Log.e(TAG, "Échec exact de l'insertion Room : " + error, error);
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
