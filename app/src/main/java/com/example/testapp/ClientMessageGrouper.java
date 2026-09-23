package com.example.testapp;

import com.example.testapp.database.SmsMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds the client presentation directly from the SMS source of truth. */
final class ClientMessageGrouper {
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
