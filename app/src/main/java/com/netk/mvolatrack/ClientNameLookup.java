package com.netk.mvolatrack;

import com.netk.mvolatrack.database.SmsMessage;
import com.netk.mvolatrack.sms.ClientNumberNormalizer;
import com.netk.mvolatrack.sms.MvolaMessageParser;

import java.util.List;

/** Finds the latest locally known client name without creating another source of truth. */
final class ClientNameLookup {
    private ClientNameLookup() {}

    static String findClientNameByPhone(List<SmsMessage> messages, String phone) {
        String normalizedPhone = ClientNumberNormalizer.normalize(phone);
        if (normalizedPhone == null || messages == null) return "-";

        String latestName = null;
        long latestDate = Long.MIN_VALUE;
        long latestId = Long.MIN_VALUE;
        for (SmsMessage message : messages) {
            MvolaMessageParser.ParsedTransaction transaction = MvolaMessageParser.parse(
                    message.messageBody, message.receivedDate);
            if (transaction == null || !normalizedPhone.equals(transaction.clientNumber)) continue;

            String name = transaction.clientName == null ? "" : transaction.clientName.trim();
            if (name.isEmpty() || "-".equals(name)) continue;
            if (message.receivedDate > latestDate
                    || (message.receivedDate == latestDate && message.id > latestId)) {
                latestName = name;
                latestDate = message.receivedDate;
                latestId = message.id;
            }
        }
        return latestName == null ? "-" : latestName;
    }
}
