package com.netk.mvolatrack.account;

/** Validation and display formatting for the cash point's own Malagasy phone number. */
public final class CashPointNumber {
    private CashPointNumber() {}

    /** Returns a canonical national number, preserving its leading zero, or {@code null}. */
    public static String normalize(String value) {
        if (value == null) return null;
        String compact = value.trim().replaceAll("\\s+", "");
        if (compact.matches("0\\d{9}")) return compact;
        if (compact.matches("\\+261\\d{9}")) return "0" + compact.substring(4);
        return null;
    }

    public static boolean isValid(String value) {
        return normalize(value) != null;
    }

    public static String format(String value) {
        String number = normalize(value);
        if (number == null) return value == null ? "" : value;
        return number.substring(0, 3) + " " + number.substring(3, 5) + " "
                + number.substring(5, 8) + " " + number.substring(8);
    }
}
