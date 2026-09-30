package com.netk.mvolatrack;

import com.netk.mvolatrack.database.SmsMessage;
import com.netk.mvolatrack.sms.MvolaMessageParser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/** Builds the client presentation directly from the SMS source of truth. */
final class ClientMessageGrouper {
    enum Filter { ALL, NEW, EXISTING }

    private ClientMessageGrouper() {}

    static List<ClientGroup> group(List<SmsMessage> messages) {
        List<SmsMessage> newestFirst = new ArrayList<>(messages == null
                ? Collections.emptyList() : messages);
        newestFirst.sort((left, right) -> {
            int byDate = Long.compare(right.receivedDate, left.receivedDate);
            return byDate != 0 ? byDate : Long.compare(right.id, left.id);
        });

        Map<String, List<SmsMessage>> grouped = new LinkedHashMap<>();
        for (SmsMessage message : newestFirst) {
            String clientNumber = clientKey(message);
            if (clientNumber != null) {
                grouped.computeIfAbsent(clientNumber, ignored -> new ArrayList<>()).add(message);
            }
        }

        List<ClientGroup> result = new ArrayList<>();
        for (Map.Entry<String, List<SmsMessage>> entry : grouped.entrySet()) {
            result.add(new ClientGroup(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    static String clientKey(SmsMessage message) {
        MvolaMessageParser.ParsedTransaction parsed = MvolaMessageParser.parse(
                message.messageBody, message.receivedDate);
        return parsed == null ? null : parsed.clientNumber;
    }

    /** Filters the derived presentation without changing its newest-message-first order. */
    static List<ClientGroup> filter(List<ClientGroup> clients, String query, Filter filter) {
        return filter(clients, query, filter, System.currentTimeMillis(), TimeZone.getDefault());
    }

    /** Date-aware entry point: a client is new only on its first transaction's local day. */
    static List<ClientGroup> filter(List<ClientGroup> clients, String query, Filter filter,
                                    long nowMillis, TimeZone timeZone) {
        String normalizedQuery = normalizeNumber(query);
        boolean emptyQuery = query == null || query.trim().isEmpty();
        List<ClientGroup> result = new ArrayList<>();
        for (ClientGroup client : clients) {
            boolean firstSeenToday = isSameLocalDay(client.firstAppearanceDate(), nowMillis,
                    timeZone);
            boolean matchesFilter = filter == Filter.ALL
                    || (filter == Filter.NEW && firstSeenToday)
                    || (filter == Filter.EXISTING && !firstSeenToday);
            if (matchesFilter && matchesSearch(client.sender, normalizedQuery, emptyQuery)) {
                result.add(client);
            }
        }
        return result;
    }

    private static String normalizeNumber(String value) {
        return SmsDateFilter.normalizeNumber(value);
    }

    private static boolean matchesSearch(String sender, String normalizedQuery,
                                         boolean emptyQuery) {
        if (emptyQuery) return true;
        if (normalizedQuery.isEmpty()) return false;
        String normalizedSender = normalizeNumber(sender);
        if (normalizedSender.contains(normalizedQuery)) return true;
        if (!isDigitsOnly(normalizedSender) || !isDigitsOnly(normalizedQuery)) return false;

        String localSender = toMalagasyLocalNumber(normalizedSender);
        String localQuery = toMalagasyLocalNumber(normalizedQuery);
        return localSender.contains(localQuery)
                || withoutLeadingZero(localSender).contains(withoutLeadingZero(localQuery));
    }

    private static boolean isDigitsOnly(String value) {
        return !value.isEmpty() && value.matches("\\d+");
    }

    /** Converts an international Malagasy search representation without altering stored data. */
    private static String toMalagasyLocalNumber(String value) {
        return value.startsWith("261") ? "0" + value.substring(3) : value;
    }

    private static String withoutLeadingZero(String value) {
        return value.length() > 1 && value.startsWith("0") ? value.substring(1) : value;
    }

    private static boolean isSameLocalDay(long leftMillis, long rightMillis, TimeZone timeZone) {
        Calendar left = Calendar.getInstance(timeZone);
        left.setTimeInMillis(leftMillis);
        Calendar right = Calendar.getInstance(timeZone);
        right.setTimeInMillis(rightMillis);
        return left.get(Calendar.ERA) == right.get(Calendar.ERA)
                && left.get(Calendar.YEAR) == right.get(Calendar.YEAR)
                && left.get(Calendar.DAY_OF_YEAR) == right.get(Calendar.DAY_OF_YEAR);
    }

    static final class ClientGroup {
        final String sender;
        final List<SmsMessage> messages;

        ClientGroup(String sender, List<SmsMessage> messages) {
            this.sender = sender;
            this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
        }

        long latestDate() {
            return messages.isEmpty() ? 0L : messages.get(0).receivedDate;
        }

        long firstAppearanceDate() {
            long earliest = Long.MAX_VALUE;
            for (SmsMessage message : messages) {
                MvolaMessageParser.ParsedTransaction parsed = MvolaMessageParser.parse(
                        message.messageBody, message.receivedDate);
                long transactionDate = parsed == null ? message.receivedDate : parsed.transactionAt;
                earliest = Math.min(earliest, transactionDate);
            }
            return earliest == Long.MAX_VALUE ? 0L : earliest;
        }
    }
}
