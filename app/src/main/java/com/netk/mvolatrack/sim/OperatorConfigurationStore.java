package com.netk.mvolatrack.sim;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Persistent store for the operator chosen in Settings. */
public final class OperatorConfigurationStore {
    private static final String FILE = "operator_verification";
    private static final String EXPECTED_NAME = "expected_name";
    private static final String ACCEPTED_NAMES = "accepted_names";
    private static final String NETWORK_ID = "network_id";

    private final SharedPreferences preferences;

    public OperatorConfigurationStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public OperatorConfiguration load() {
        Set<String> stored = preferences.getStringSet(ACCEPTED_NAMES, new LinkedHashSet<>());
        return new OperatorConfiguration(preferences.getString(EXPECTED_NAME, ""),
                new ArrayList<>(stored), preferences.getString(NETWORK_ID, ""));
    }

    public void save(String expectedName, List<String> acceptedNames, String networkId) {
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        for (String name : acceptedNames) {
            if (name != null && !name.trim().isEmpty()) cleaned.add(name.trim());
        }
        preferences.edit()
                .putString(EXPECTED_NAME, expectedName == null ? "" : expectedName.trim())
                .putStringSet(ACCEPTED_NAMES, cleaned)
                .putString(NETWORK_ID, networkId == null ? "" : networkId.trim())
                .apply();
    }
}
