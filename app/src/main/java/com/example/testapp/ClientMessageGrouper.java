package com.example.testapp;

import com.example.testapp.database.SmsMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
            grouped.computeIfAbsent(message.sender, ignored -> new ArrayList<>()).add(message);
        }

        List<ClientGroup> result = new ArrayList<>();
        for (Map.Entry<String, List<SmsMessage>> entry : grouped.entrySet()) {
            result.add(new ClientGroup(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    /** Filters the derived presentation without changing its newest-message-first order. */
    static List<ClientGroup> filter(List<ClientGroup> clients, String query, Filter filter) {
        String normalizedQuery = normalizeForSearch(query);
        List<ClientGroup> result = new ArrayList<>();
        for (ClientGroup client : clients) {
            int messageCount = client.messages.size();
            boolean matchesFilter = filter == Filter.ALL
                    || (filter == Filter.NEW && messageCount == 1)
                    || (filter == Filter.EXISTING && messageCount >= 2);
            if (matchesFilter
                    && normalizeForSearch(client.sender).contains(normalizedQuery)) {
                result.add(client);
            }
        }
        return result;
    }

    private static String normalizeForSearch(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
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
    }
}
