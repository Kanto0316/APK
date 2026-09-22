package com.example.testapp;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Merges, de-duplicates and ranks cached contacts and local recent recipients. */
final class DepositRecipientSearch {
    private DepositRecipientSearch() {}

    static List<DepositRecipient> find(List<DepositRecipient> contacts,
            List<DepositRecipient> recent, String query, int limit) {
        Map<String, DepositRecipient> merged = new HashMap<>();
        for (DepositRecipient item : recent) merged.put(item.number, item);
        for (DepositRecipient contact : contacts) {
            DepositRecipient old = merged.get(contact.number);
            merged.put(contact.number, new DepositRecipient(contact.number,
                    contact.hasName() ? contact.name : old == null ? "" : old.name,
                    old == null ? 0L : old.lastUsed, true));
        }

        String text = fold(query == null ? "" : query.trim());
        String digits = searchableNumber(query);
        List<DepositRecipient> matches = new ArrayList<>();
        for (DepositRecipient item : merged.values()) {
            if (text.isEmpty() ? item.lastUsed > 0 : matches(item, text, digits)) matches.add(item);
        }
        matches.sort((left, right) -> {
            int rank = Integer.compare(rank(left, text, digits), rank(right, text, digits));
            if (rank != 0) return rank;
            int recentOrder = Long.compare(right.lastUsed, left.lastUsed);
            if (recentOrder != 0) return recentOrder;
            return left.name.compareToIgnoreCase(right.name);
        });
        return new ArrayList<>(matches.subList(0, Math.min(limit, matches.size())));
    }

    private static boolean matches(DepositRecipient item, String text, String digits) {
        return fold(item.name).contains(text)
                || (!digits.isEmpty() && (item.number.contains(digits)
                || international(item.number).contains(digits)));
    }

    private static int rank(DepositRecipient item, String text, String digits) {
        if (!digits.isEmpty() && (item.number.equals(digits)
                || international(item.number).equals(digits))) return 0;
        if (item.lastUsed > 0) return 1;
        if (!text.isEmpty() && fold(item.name).startsWith(text)) return 2;
        if (!digits.isEmpty() && (item.number.startsWith(digits)
                || international(item.number).startsWith(digits))) return 3;
        return 4;
    }

    private static String searchableNumber(String value) {
        if (value == null) return "";
        String compact = value.replaceAll("[^+0-9]", "");
        if (compact.startsWith("+261")) return "0" + compact.substring(4);
        return compact.replace("+", "");
    }

    private static String international(String national) {
        return "+261" + national.substring(1);
    }

    private static String fold(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
