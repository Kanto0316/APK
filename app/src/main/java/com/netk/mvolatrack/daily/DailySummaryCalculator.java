package com.netk.mvolatrack.daily;

import com.netk.mvolatrack.database.SmsMessage;
import com.netk.mvolatrack.sms.ClientNumberNormalizer;
import com.netk.mvolatrack.sms.MvolaMessageParser;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/** Calculates a business-day summary directly from deduplicated stored SMS rows. */
public final class DailySummaryCalculator {
    public static final TimeZone BUSINESS_ZONE = TimeZone.getTimeZone("Indian/Antananarivo");
    private DailySummaryCalculator() {}

    public static long startOfDay(long instant) {
        Calendar c = Calendar.getInstance(BUSINESS_ZONE);
        c.setTimeInMillis(instant);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static long previousDayStart(long now) {
        Calendar c = Calendar.getInstance(BUSINESS_ZONE);
        c.setTimeInMillis(startOfDay(now)); c.add(Calendar.DAY_OF_MONTH, -1);
        return c.getTimeInMillis();
    }

    public static long nextDay(long dayStart) {
        Calendar c = Calendar.getInstance(BUSINESS_ZONE);
        c.setTimeInMillis(dayStart); c.add(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis();
    }

    public static DailySummary calculate(List<SmsMessage> messages, long dayStart) {
        DailySummary out = new DailySummary(dayStart);
        long end = nextDay(dayStart);
        Set<String> clients = new HashSet<>(), seen = new HashSet<>();
        if (messages == null) return out;
        for (SmsMessage sms : messages) {
            if (sms == null || !seen.add(sms.uniqueKey)) continue;
            MvolaMessageParser.ParsedTransaction tx =
                    MvolaMessageParser.parse(sms.messageBody, sms.receivedDate, BUSINESS_ZONE);
            if (tx == null || tx.transactionAt < dayStart || tx.transactionAt >= end) continue;
            out.transactions++;
            String number = ClientNumberNormalizer.normalize(tx.clientNumber);
            if (number != null) clients.add(number);
            if ("Dépôt".equals(tx.type)) { out.deposits++; out.depositAmount += tx.amount; }
            else if ("Retrait".equals(tx.type)) { out.withdrawals++; out.withdrawalAmount += tx.amount; }
            else if ("Crédit".equals(tx.type)) { out.credits++; out.creditAmount += tx.amount; }
            if (tx.bonus == null) out.bonusPartial = true; else out.bonus += tx.bonus;
        }
        out.clients = clients.size();
        return out;
    }
}
