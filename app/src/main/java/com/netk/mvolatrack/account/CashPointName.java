package com.netk.mvolatrack.account;

/** Validation shared by the account screen and its tests. */
public final class CashPointName {
    public static final int MAX_LENGTH = 18;

    private CashPointName() {}

    public static String normalize(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("\\s+", " ");
    }

    public static boolean isValid(String value) {
        String normalized = normalize(value);
        return !normalized.isEmpty() && normalized.length() <= MAX_LENGTH;
    }
}
