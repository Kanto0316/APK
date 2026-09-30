package com.netk.mvolatrack;

import com.netk.mvolatrack.database.SmsMessage;
import com.netk.mvolatrack.sms.MvolaMessageParser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

/** Pure display filtering for the messages table; stored messages are never mutated. */
final class SmsDateFilter {
    enum Period {
        ALL, TODAY, YESTERDAY, SEVEN_DAYS, THIRTY_DAYS, CUSTOM_DATE,
        THIS_WEEK, THIS_MONTH, CUSTOM_RANGE
    }
    enum TransactionType {
        ALL(null), DEPOSIT("Dépôt"), WITHDRAWAL("Retrait"), CREDIT("Crédit"),
        CREDIT_ENTRY(null), DEBIT_EXIT(null);

        final String parsedType;

        TransactionType(String parsedType) {
            this.parsedType = parsedType;
        }
    }

    static final class DisplayMessage {
        final SmsMessage message;
        final int originalNumber;

        DisplayMessage(SmsMessage message, int originalNumber) {
            this.message = message;
            this.originalNumber = originalNumber;
        }
    }

    private SmsDateFilter() { }

    static List<DisplayMessage> apply(List<SmsMessage> source, Period period,
                                      Long customDateMillis, long nowMillis, TimeZone timeZone) {
        return apply(source, period, customDateMillis, nowMillis, timeZone, "");
    }

    static List<DisplayMessage> apply(List<SmsMessage> source, Period period,
                                      Long customDateMillis, long nowMillis, TimeZone timeZone,
                                      String query) {
        return apply(source, period, customDateMillis, nowMillis, timeZone, query,
                TransactionType.ALL);
    }

    /** Range-aware entry point used by exports; all transaction parsing and matching stays here. */
    static List<DisplayMessage> apply(List<SmsMessage> source, Period period,
                                      Long customStartMillis, Long customEndMillis,
                                      long nowMillis, TimeZone timeZone, String query,
                                      TransactionType transactionType) {
        long[] bounds = bounds(period, customStartMillis, customEndMillis, nowMillis, timeZone);
        return applyBounds(source, bounds[0], bounds[1], query, transactionType);
    }

    static List<DisplayMessage> apply(List<SmsMessage> source, Period period,
                                      Long customDateMillis, long nowMillis, TimeZone timeZone,
                                      String query, TransactionType transactionType) {
        long[] bounds = bounds(period, customDateMillis, customDateMillis, nowMillis, timeZone);
        return applyBounds(source, bounds[0], bounds[1], query, transactionType);
    }

    private static long[] bounds(Period period, Long customStartMillis, Long customEndMillis,
                                 long nowMillis, TimeZone timeZone) {
        long start = Long.MIN_VALUE, end = Long.MAX_VALUE;
        if (period != Period.ALL) {
            Calendar selectedDay = Calendar.getInstance(timeZone);
            selectedDay.setTimeInMillis((period == Period.CUSTOM_DATE
                    || period == Period.CUSTOM_RANGE) && customStartMillis != null
                    ? customStartMillis : nowMillis);
            startOfDay(selectedDay);
            if (period == Period.YESTERDAY) selectedDay.add(Calendar.DAY_OF_MONTH, -1);
            else if (period == Period.SEVEN_DAYS) selectedDay.add(Calendar.DAY_OF_MONTH, -6);
            else if (period == Period.THIRTY_DAYS) selectedDay.add(Calendar.DAY_OF_MONTH, -29);
            else if (period == Period.THIS_WEEK) {
                selectedDay.setFirstDayOfWeek(Calendar.MONDAY);
                selectedDay.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            } else if (period == Period.THIS_MONTH) selectedDay.set(Calendar.DAY_OF_MONTH, 1);
            start = selectedDay.getTimeInMillis();

            Calendar tomorrow = Calendar.getInstance(timeZone);
            tomorrow.setTimeInMillis(period == Period.CUSTOM_RANGE && customEndMillis != null
                    ? customEndMillis : ((period == Period.CUSTOM_DATE
                    && customStartMillis != null) ? customStartMillis : nowMillis));
            startOfDay(tomorrow);
            if (period == Period.YESTERDAY) {
                // The exclusive end of yesterday is the start of today.
            } else tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            end = tomorrow.getTimeInMillis();
        }
        return new long[]{start, end};
    }

    private static List<DisplayMessage> applyBounds(List<SmsMessage> source, long start, long end,
            String query, TransactionType transactionType) {
        if (source == null || source.isEmpty()) return Collections.emptyList();

        List<DisplayMessage> result = new ArrayList<>();
        int total = source.size();
        String normalizedNumberQuery = normalizeNumber(query);
        boolean emptyQuery = query == null || query.trim().isEmpty();
        for (int index = 0; index < total; index++) {
            SmsMessage message = source.get(index);
            // The global source is broader than the already-curated client adapter snapshot.
            // Imports or a refresh may therefore expose an incomplete row. Treat it exactly like
            // an unrecognised SMS instead of crashing before the shared export pipeline is called.
            if (message == null || message.messageBody == null) continue;
            MvolaMessageParser.ParsedTransaction parsed =
                    MvolaMessageParser.parse(message.messageBody, message.receivedDate);
            // A persisted SMS is not necessarily a business transaction. Keep unrecognised
            // messages in Room for future reprocessing, but never expose them in this view.
            if (parsed == null) continue;
            boolean typeMatches = matchesType(transactionType, parsed.type);
            String searchableNumber = parsed.clientNumber;
            long effectiveDate = parsed.transactionAt;
            boolean numberMatches = !normalizedNumberQuery.isEmpty()
                    && normalizeNumber(SmsDisplayFormatter.sender(searchableNumber))
                    .contains(normalizedNumberQuery);
            if (effectiveDate >= start && effectiveDate < end && typeMatches
                    && (emptyQuery || numberMatches)) {
                result.add(new DisplayMessage(message, total - index));
            }
        }
        return result;
    }

    private static boolean matchesType(TransactionType selected, String parsedType) {
        if (selected == TransactionType.ALL) return true;
        if (selected == TransactionType.CREDIT_ENTRY) {
            return "Crédit".equals(parsedType) || "Dépôt".equals(parsedType);
        }
        if (selected == TransactionType.DEBIT_EXIT) return "Retrait".equals(parsedType);
        return selected.parsedType.equals(parsedType);
    }

    /** Keeps digits only; phone numbers remain strings so leading zeroes are retained. */
    static String normalizeNumber(String value) {
        if (value == null || value.isEmpty()) return "";
        StringBuilder normalized = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isDigit(character)) normalized.append(character);
        }
        return normalized.toString();
    }

    private static void startOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }
}
