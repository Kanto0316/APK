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

        // Acquire the PendingResult before doing any work. A manifest receiver can start a fresh
        // process with no Activity, and this is the token that keeps that process eligible to run
        // until the asynchronous Room insertion has completed.
        PendingResult pendingResult = goAsync();
        Log.i(TAG, "SMS Receiver déclenché");
        Context appContext = context.getApplicationContext();
        try {
            SmsReceptionDiagnostics.recordReceiverInvocation(
                    appContext, System.currentTimeMillis());

            Bundle extras = intent.getExtras();
            Object rawPdusValue = extras == null ? null : extras.get("pdus");
            Object[] rawPdus = rawPdusValue instanceof Object[] ? (Object[]) rawPdusValue : null;
            int bundlePartCount = rawPdus == null ? 0 : rawPdus.length;
            Log.i(TAG, "Nombre de PDUs reçu : " + bundlePartCount);

            SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            if (parts == null || parts.length == 0) {
                Log.w(TAG, "SMS_RECEIVED sans segment décodable");
                pendingResult.finish();
                return;
            }
            Log.i(TAG, "Nombre de PDUs décodé : " + parts.length);

            StringBuilder completeBody = new StringBuilder();
            SmsMessage first = null;
            for (SmsMessage part : parts) {
                if (part != null && first == null) first = part;
                if (part != null && part.getMessageBody() != null) {
                    completeBody.append(part.getMessageBody());
                }
            }
            if (first == null) {
                Log.w(TAG, "Tous les segments SMS sont nuls, réception ignorée");
                pendingResult.finish();
                return;
            }

            String sender = first.getDisplayOriginatingAddress();
            long receivedAt = first.getTimestampMillis();
            String body = completeBody.toString();
            // Parse exactly once at the reception boundary. The same immutable result is passed
            // to the presentation coordinator after Room has accepted the SMS.
            MvolaMessageParser.ParsedTransaction parsedTransaction =
                    MvolaMessageParser.parse(body, receivedAt);
            Log.d(TAG, "SMS extrait ; date=" + receivedAt + ", longueur=" + body.length());

            com.example.testapp.database.SmsMessage localMessage =
                    com.example.testapp.database.SmsMessage.create(
                            sender, body, receivedAt, false);
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
                            Log.i(TAG, "SMS enregistré Room");
                            Log.i(TAG, "Insertion Room réussie ; rowId=" + rowId + " ("
                                    + parts.length + " segment(s))");
                            if (parsedTransaction != null) {
                                com.example.testapp.overlay.TransactionOverlayCoordinator
                                        .get(appContext).show(parsedTransaction);
                            }
                        }
                    } else {
                        Log.e(TAG, "Échec exact de l'insertion Room : " + error, error);
                    }
                } finally {
                    Log.d(TAG, "Insertion Room terminée ; libération de goAsync()");
                    pendingResult.finish();
                }
            });
        } catch (RuntimeException receiverFailure) {
            Log.e(TAG, "Impossible de traiter ou planifier le SMS", receiverFailure);
            pendingResult.finish();
        }
    }
}
