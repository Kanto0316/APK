package com.netk.mvolatrack.history;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/** Totals the already-built history projection for one local calendar day. */
public final class TodayHistorySummary {
    public final long incoming;
    public final long outgoing;

    private TodayHistorySummary(long incoming, long outgoing) {
        this.incoming = incoming;
        this.outgoing = outgoing;
    }

    public static TodayHistorySummary calculate(List<HistoryTransaction> transactions,
                                                 long now, TimeZone timeZone) {
        Calendar day = Calendar.getInstance(timeZone);
        day.setTimeInMillis(now);
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        long start = day.getTimeInMillis();
        day.add(Calendar.DAY_OF_MONTH, 1);
        long end = day.getTimeInMillis();

        long incoming = 0;
        long outgoing = 0;
        if (transactions != null) {
            for (HistoryTransaction transaction : transactions) {
                if (transaction.timestamp < start || transaction.timestamp >= end) continue;
                long amount = Math.abs(transaction.amount);
                if ("Retrait".equalsIgnoreCase(transaction.type)) {
                    incoming += amount;
                } else if ("Dépôt".equalsIgnoreCase(transaction.type)
                        || "Crédit".equalsIgnoreCase(transaction.type)) {
                    outgoing += amount;
                }
            }
        }
        return new TodayHistorySummary(incoming, outgoing);
    }
}
