package com.example.testapp;

import com.example.testapp.database.SmsMessage;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

/** Builds daily statistics from the timestamp already stored on each SMS. */
final class SmsStatistics {
    private SmsStatistics() {}

    static List<DailyCount> groupByLocalDate(List<SmsMessage> messages) {
        return groupByDate(messages, TimeZone.getDefault());
    }

    static List<DailyCount> groupByDate(List<SmsMessage> messages, TimeZone timeZone) {
        if (messages == null || messages.isEmpty()) return Collections.emptyList();

        Calendar calendar = Calendar.getInstance(timeZone);
        Map<Long, Integer> counts = new TreeMap<>();
        for (SmsMessage message : messages) {
            calendar.setTimeInMillis(message.receivedDate);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long localDay = calendar.getTimeInMillis();
            counts.put(localDay, counts.getOrDefault(localDay, 0) + 1);
        }

        List<DailyCount> result = new ArrayList<>(counts.size());
        for (Map.Entry<Long, Integer> entry : counts.entrySet()) {
            result.add(new DailyCount(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    static final class DailyCount {
        final long localDayTimestamp;
        final int count;

        DailyCount(long localDayTimestamp, int count) {
            this.localDayTimestamp = localDayTimestamp;
            this.count = count;
        }
    }
}
