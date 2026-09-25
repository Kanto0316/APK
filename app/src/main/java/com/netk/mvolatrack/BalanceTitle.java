package com.netk.mvolatrack;

import com.netk.mvolatrack.database.SmsMessage;
import com.netk.mvolatrack.sms.MvolaMessageParser;

import java.util.List;
import java.util.Locale;

/** Builds the displayed balance amount from parsed MVola SMS messages. */
final class BalanceTitle {
    private BalanceTitle() {}

    static String from(List<SmsMessage> messages) {
        long balance = latestBalance(messages);
        String formatted = String.format(Locale.FRENCH, "%,d", balance)
                .replace('\u00a0', ' ').replace('\u202f', ' ');
        return formatted + " Ar";
    }

    /** Returns the balance carried by the most recently received eligible SMS. */
    static long latestBalance(List<SmsMessage> messages) {
        long latestReceivedDate = Long.MIN_VALUE;
        long latestId = Long.MIN_VALUE;
        Long latestBalance = null;

        if (messages != null) {
            for (SmsMessage message : messages) {
                if (message == null) continue;
                MvolaMessageParser.ParsedTransaction transaction =
                        MvolaMessageParser.parse(message.messageBody, message.receivedDate);
                if (transaction == null || transaction.balance == null) continue;

                // The title reflects arrival order, not the business date embedded in the SMS.
                if (message.receivedDate > latestReceivedDate
                        || (message.receivedDate == latestReceivedDate && message.id > latestId)) {
                    latestReceivedDate = message.receivedDate;
                    latestId = message.id;
                    latestBalance = transaction.balance;
                }
            }
        }
        return latestBalance == null ? 0 : latestBalance;
    }
}
