package com.netk.mvolatrack.sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** User-defined identity of the mobile operator expected by the application. */
public final class OperatorConfiguration {
    public final String expectedName;
    public final List<String> acceptedNames;
    public final String networkId;

    public OperatorConfiguration(String expectedName, List<String> acceptedNames, String networkId) {
        this.expectedName = clean(expectedName);
        this.acceptedNames = Collections.unmodifiableList(new ArrayList<>(acceptedNames));
        this.networkId = clean(networkId);
    }

    public boolean isConfigured() {
        return !expectedName.isEmpty();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
