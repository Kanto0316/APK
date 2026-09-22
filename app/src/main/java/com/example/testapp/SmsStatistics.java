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

    /** Builds a continuous calendar month in one pass over the available messages. */
    static List<DailyCount> forMonth(List<SmsMessage> messages, int year, int month,
            TimeZone timeZone) {
        Calendar day = Calendar.getInstance(timeZone);
        day.clear();
        day.set(year, month, 1);
        int dayCount = day.getActualMaximum(Calendar.DAY_OF_MONTH);
        int[] counts = new int[dayCount];

        if (messages != null) {
            Calendar received = Calendar.getInstance(timeZone);
            for (SmsMessage message : messages) {
                received.setTimeInMillis(message.receivedDate);
                if (received.get(Calendar.YEAR) == year && received.get(Calendar.MONTH) == month) {
                    counts[received.get(Calendar.DAY_OF_MONTH) - 1]++;
                }
            }
        }

        List<DailyCount> result = new ArrayList<>(dayCount);
        for (int index = 0; index < dayCount; index++) {
            day.set(Calendar.DAY_OF_MONTH, index + 1);
            result.add(new DailyCount(day.getTimeInMillis(), counts[index]));
        }
        return result;
    }

    static boolean hasActivity(List<DailyCount> dailyCounts) {
        for (DailyCount dailyCount : dailyCounts) {
            if (dailyCount.count > 0) return true;
        }
        return false;
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
