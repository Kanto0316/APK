package com.example.testapp;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Private, app-local deposit recipient history. Amounts are deliberately never stored. */
final class DepositRecipientHistory {
    private static final String PREFERENCES = "deposit_recipient_history";
    private static final String ENTRIES = "entries";
    private static final int MAX_STORED = 20;
    private final SharedPreferences preferences;

    DepositRecipientHistory(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    List<DepositRecipient> load() {
        List<DepositRecipient> result = new ArrayList<>();
        try {
            JSONArray entries = new JSONArray(preferences.getString(ENTRIES, "[]"));
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.getJSONObject(i);
                String number = DepositUssd.normalizeRecipientNumber(entry.optString("number"));
                if (number != null) {
                    result.add(new DepositRecipient(number, entry.optString("name"),
                            entry.optLong("lastUsed"), false));
                }
            }
        } catch (JSONException ignored) {
            // A damaged private preference is treated as an empty history.
        }
        result.sort((left, right) -> Long.compare(right.lastUsed, left.lastUsed));
        return result;
    }

    void record(String number, String name) {
        String normalized = DepositUssd.normalizeRecipientNumber(number);
        if (normalized == null) return;
        List<DepositRecipient> entries = load();
        entries.removeIf(entry -> entry.number.equals(normalized));
        entries.add(new DepositRecipient(normalized, name, System.currentTimeMillis(), false));
        entries.sort(Comparator.comparingLong((DepositRecipient entry) -> entry.lastUsed).reversed());

        JSONArray json = new JSONArray();
        for (int i = 0; i < Math.min(entries.size(), MAX_STORED); i++) {
            DepositRecipient entry = entries.get(i);
            JSONObject value = new JSONObject();
            try {
                value.put("number", entry.number);
                value.put("name", entry.name);
                value.put("lastUsed", entry.lastUsed);
                json.put(value);
            } catch (JSONException ignored) {
                // These primitive values are JSON-safe.
            }
        }
        preferences.edit().putString(ENTRIES, json.toString()).apply();
    }
}
