package com.netk.mvolatrack.history;

import com.netk.mvolatrack.database.SmsMessage;
import com.netk.mvolatrack.sms.MvolaMessageParser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Display projection of a transaction parsed from the application's persisted SMS source. */
public final class HistoryTransaction {
    public final String type;
    public final String clientNumber;
    public final long amount;
    public final Long bonus;
    public final String reference;
    public final long timestamp;

    private HistoryTransaction(String type, String clientNumber, long amount, Long bonus,
                               String reference, long timestamp) {
        this.type = type;
        this.clientNumber = clientNumber;
        this.amount = amount;
        this.bonus = bonus;
        this.reference = reference;
        this.timestamp = timestamp;
    }

    /**
     * Uses the same stored SMS and central parser as Messages and Statistics. No history data is
     * persisted separately. The business timestamp wins, with SMS reception time as fallback.
     */
    public static List<HistoryTransaction> fromMessages(List<SmsMessage> messages) {
        List<HistoryTransaction> result = new ArrayList<>();
        if (messages == null) return result;
        for (SmsMessage message : messages) {
            MvolaMessageParser.ParsedTransaction parsed =
                    MvolaMessageParser.parse(message.messageBody, message.receivedDate);
            if (parsed == null || parsed.clientNumber == null) continue;
            long timestamp = parsed.transactionAt > 0
                    ? parsed.transactionAt : message.receivedDate;
            result.add(new HistoryTransaction(parsed.type, parsed.clientNumber, parsed.amount,
                    parsed.bonus, parsed.reference, timestamp));
        }
        result.sort(Comparator.comparingLong((HistoryTransaction item) -> item.timestamp)
                .reversed());
        return result;
    }
}
