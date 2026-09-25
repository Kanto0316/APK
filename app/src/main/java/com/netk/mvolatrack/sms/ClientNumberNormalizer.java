package com.netk.mvolatrack.sms;

/** Canonicalises Malagasy mobile numbers for grouping, searching and statistics. */
public final class ClientNumberNormalizer {
    private ClientNumberNormalizer() {}

    public static String normalize(String value) {
        if (value == null) return null;
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.startsWith("261") && digits.length() == 12) {
            digits = "0" + digits.substring(3);
        }
        return digits.matches("0\\d{9}") ? digits : null;
    }

    public static String format(String value) {
        String number = normalize(value);
        if (number == null) return value;
        return number.substring(0, 3) + " " + number.substring(3, 5) + " "
                + number.substring(5, 8) + " " + number.substring(8);
    }
}
