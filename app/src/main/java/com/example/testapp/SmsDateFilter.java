package com.example.testapp;

import com.example.testapp.database.SmsMessage;
import com.example.testapp.sms.MvolaMessageParser;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Pure display filtering for the messages table; stored messages are never mutated. */
final class SmsDateFilter {
    enum Period { ALL, TODAY, YESTERDAY, SEVEN_DAYS, THIRTY_DAYS, CUSTOM_DATE }

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
        if (source == null || source.isEmpty()) return Collections.emptyList();

        long start = Long.MIN_VALUE;
        long end = Long.MAX_VALUE;
        if (period != Period.ALL) {
            Calendar selectedDay = Calendar.getInstance(timeZone);
            selectedDay.setTimeInMillis(period == Period.CUSTOM_DATE && customDateMillis != null
                    ? customDateMillis : nowMillis);
            startOfDay(selectedDay);
            if (period == Period.YESTERDAY) selectedDay.add(Calendar.DAY_OF_MONTH, -1);
            else if (period == Period.SEVEN_DAYS) selectedDay.add(Calendar.DAY_OF_MONTH, -6);
            else if (period == Period.THIRTY_DAYS) selectedDay.add(Calendar.DAY_OF_MONTH, -29);
            start = selectedDay.getTimeInMillis();

            Calendar tomorrow = Calendar.getInstance(timeZone);
            tomorrow.setTimeInMillis(period == Period.CUSTOM_DATE && customDateMillis != null
                    ? customDateMillis : nowMillis);
            startOfDay(tomorrow);
            if (period == Period.YESTERDAY) {
                // The exclusive end of yesterday is the start of today.
            } else {
                tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            }
            end = tomorrow.getTimeInMillis();
        }

        List<DisplayMessage> result = new ArrayList<>();
        int total = source.size();
        String normalizedNumberQuery = normalizeNumber(query);
        String normalizedTextQuery = normalizeText(query);
        boolean emptyQuery = query == null || query.trim().isEmpty();
        for (int index = 0; index < total; index++) {
            SmsMessage message = source.get(index);
            MvolaMessageParser.ParsedTransaction parsed =
                    MvolaMessageParser.parse(message.messageBody, message.receivedDate);
            // A persisted SMS is not necessarily a business transaction. Keep unrecognised
            // messages in Room for future reprocessing, but never expose them in this view.
            if (parsed == null) continue;
            String searchableNumber = parsed.clientNumber;
            long effectiveDate = parsed.transactionAt;
            boolean numberMatches = !normalizedNumberQuery.isEmpty()
                    && normalizeNumber(SmsDisplayFormatter.sender(searchableNumber))
                    .contains(normalizedNumberQuery);
            String searchableName = parsed.clientName;
            boolean nameMatches = !normalizedTextQuery.isEmpty() && searchableName != null
                    && !searchableName.trim().isEmpty()
                    && !"-".equals(searchableName.trim())
                    && normalizeText(searchableName).contains(normalizedTextQuery);
            if (effectiveDate >= start && effectiveDate < end
                    && (emptyQuery || numberMatches || nameMatches)) {
                result.add(new DisplayMessage(message, total - index));
            }
        }
        return result;
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

    /** Provides case- and accent-insensitive matching without searching the raw SMS body. */
    static String normalizeText(String value) {
        if (value == null || value.isEmpty()) return "";
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        StringBuilder normalized = new StringBuilder(decomposed.length());
        for (int index = 0; index < decomposed.length(); index++) {
            char character = decomposed.charAt(index);
            if (Character.getType(character) != Character.NON_SPACING_MARK) {
                normalized.append(character);
            }
        }
        return normalized.toString().toLowerCase(Locale.ROOT).trim();
    }

    private static void startOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }
}
