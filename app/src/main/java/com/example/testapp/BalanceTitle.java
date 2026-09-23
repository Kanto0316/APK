package com.example.testapp;

import com.example.testapp.database.SmsMessage;
import com.example.testapp.sms.MvolaMessageParser;

import java.util.List;
import java.util.Locale;

/** Builds the global balance title from the already parsed MVola transactions. */
final class BalanceTitle {
    private BalanceTitle() {}

    static String from(List<SmsMessage> messages) {
        long balance = latestBalance(messages);
        String formatted = String.format(Locale.FRENCH, "%,d", balance)
                .replace('\u00a0', ' ').replace('\u202f', ' ');
        return "Solde : " + formatted + " Ar >";
    }

    /** Returns the balance carried by the chronologically newest eligible transaction. */
    static long latestBalance(List<SmsMessage> messages) {
        long latestDate = Long.MIN_VALUE;
        long latestReceivedDate = Long.MIN_VALUE;
        Long latestBalance = null;

        if (messages != null) {
            for (SmsMessage message : messages) {
                if (message == null) continue;
                MvolaMessageParser.ParsedTransaction transaction =
                        MvolaMessageParser.parse(message.messageBody, message.receivedDate);
                if (transaction == null || transaction.balance == null) continue;

                // A parsed transaction date is calendar-validated by the shared parser. The SMS
                // reception date remains the fallback for formats that do not provide one.
                long effectiveDate = transaction.transactionAt > 0
                        ? transaction.transactionAt : message.receivedDate;
                if (effectiveDate > latestDate
                        || (effectiveDate == latestDate
                        && message.receivedDate > latestReceivedDate)) {
                    latestDate = effectiveDate;
                    latestReceivedDate = message.receivedDate;
                    latestBalance = transaction.balance;
                }
            }
        }
        return latestBalance == null ? 0 : latestBalance;
    }
}
