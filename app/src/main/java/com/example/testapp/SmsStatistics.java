package com.example.testapp;

import com.example.testapp.database.SmsMessage;
import com.example.testapp.sms.MvolaMessageParser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
                MvolaMessageParser.ParsedTransaction parsed =
                        MvolaMessageParser.parse(message.messageBody);
                if (parsed == null) continue;
                received.setTimeInMillis(parsed.transactionAt);
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

    /** Derives transaction, bonus and distinct-client totals from persisted SMS. */
    static TransactionSummary summarize(List<SmsMessage> messages, long now, int selectedYear,
                                        int selectedMonth, TimeZone timeZone) {
        TransactionSummary result = new TransactionSummary();
        Calendar today = Calendar.getInstance(timeZone);
        today.setTimeInMillis(now);
        startOfDay(today);
        long todayStart = today.getTimeInMillis();
        today.add(Calendar.DAY_OF_MONTH, 1);
        long tomorrowStart = today.getTimeInMillis();
        today.add(Calendar.DAY_OF_MONTH, -2);
        long yesterdayStart = today.getTimeInMillis();
        Calendar week = Calendar.getInstance(timeZone);
        week.setTimeInMillis(now);
        startOfDay(week);
        int offset = (week.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        week.add(Calendar.DAY_OF_MONTH, -offset);
        long weekStart = week.getTimeInMillis();

        if (messages == null) return result;
        Calendar transactionDate = Calendar.getInstance(timeZone);
        Map<Long, Long> selectedMonthBonusByDay = new TreeMap<>();
        for (SmsMessage message : messages) {
            MvolaMessageParser.ParsedTransaction transaction =
                    MvolaMessageParser.parse(message.messageBody);
            if (transaction == null) continue;
            long at = businessTimestamp(transaction, message);
            transactionDate.setTimeInMillis(at);
            boolean isToday = at >= todayStart && at < tomorrowStart;
            boolean isYesterday = at >= yesterdayStart && at < todayStart;
            boolean isWeek = at >= weekStart && at < tomorrowStart;
            boolean isMonth = transactionDate.get(Calendar.YEAR) == selectedYear
                    && transactionDate.get(Calendar.MONTH) == selectedMonth;
            boolean isYear = transactionDate.get(Calendar.YEAR) == selectedYear;
            result.add(transaction, isToday, isYesterday, isWeek, isMonth, isYear);
            if (isMonth && transaction.bonus != null) {
                startOfDay(transactionDate);
                long day = transactionDate.getTimeInMillis();
                selectedMonthBonusByDay.put(day, selectedMonthBonusByDay.getOrDefault(day, 0L)
                        + transaction.bonus);
            }
        }
        for (Map.Entry<Long, Long> entry : selectedMonthBonusByDay.entrySet()) {
            result.monthlyBonus.add(new DailyCount(entry.getKey(), entry.getValue()));
        }
        if (sum(result.monthlyBonus) != result.monthBonus) {
            throw new IllegalStateException("Le total bonus mensuel diffère du graphique");
        }
        return result;
    }

    static long sum(List<DailyCount> dailyValues) {
        long total = 0;
        for (DailyCount value : dailyValues) total += value.count;
        return total;
    }

    private static void startOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    /** Prefers the date parsed from the SMS body and only falls back to reception time. */
    private static long businessTimestamp(MvolaMessageParser.ParsedTransaction transaction,
                                          SmsMessage message) {
        return transaction.transactionAt > 0 ? transaction.transactionAt : message.receivedDate;
    }

    static final class TransactionSummary {
        long todayBonus, yesterdayBonus, weekBonus, monthBonus, yearBonus;
        long todayTransactions, yesterdayTransactions, weekTransactions, monthTransactions,
                yearTransactions;
        final Set<String> todayClients = new HashSet<>();
        final Set<String> yesterdayClients = new HashSet<>();
        final Set<String> weekClients = new HashSet<>();
        final Set<String> monthClients = new HashSet<>();
        final Set<String> yearClients = new HashSet<>();
        /** Daily bonus bars for the selected month, derived in the same pass as monthBonus. */
        final List<DailyCount> monthlyBonus = new ArrayList<>();

        private void add(MvolaMessageParser.ParsedTransaction transaction, boolean today,
                         boolean yesterday, boolean week, boolean month, boolean year) {
            long bonus = transaction.bonus == null ? 0 : transaction.bonus;
            if (today) {
                todayTransactions++;
                todayBonus += bonus;
                todayClients.add(transaction.clientNumber);
            }
            if (yesterday) {
                yesterdayTransactions++;
                yesterdayBonus += bonus;
                yesterdayClients.add(transaction.clientNumber);
            }
            if (week) {
                weekTransactions++;
                weekBonus += bonus;
                weekClients.add(transaction.clientNumber);
            }
            if (month) {
                monthTransactions++;
                monthBonus += bonus;
                monthClients.add(transaction.clientNumber);
            }
            if (year) {
                yearTransactions++;
                yearBonus += bonus;
                yearClients.add(transaction.clientNumber);
            }
        }
    }

    static List<DailyCount> groupByDate(List<SmsMessage> messages, TimeZone timeZone) {
        if (messages == null || messages.isEmpty()) return Collections.emptyList();

        Calendar calendar = Calendar.getInstance(timeZone);
        Map<Long, Integer> counts = new TreeMap<>();
        for (SmsMessage message : messages) {
            MvolaMessageParser.ParsedTransaction parsed =
                    MvolaMessageParser.parse(message.messageBody);
            if (parsed == null) continue;
            calendar.setTimeInMillis(parsed.transactionAt);
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
        final long count;

        DailyCount(long localDayTimestamp, long count) {
            this.localDayTimestamp = localDayTimestamp;
            this.count = count;
        }
    }
}
