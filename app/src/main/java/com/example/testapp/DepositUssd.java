package com.example.testapp;

import java.text.NumberFormat;
import java.math.BigInteger;
import java.util.Locale;

/** Pure validation and formatting helpers for the transient deposit workflow. */
final class DepositUssd {
    private DepositUssd() {}

    static String normalizeRecipientNumber(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        if (!input.matches("[+0-9\\s()\\-]+")) return null;

        String compact = input.replaceAll("[\\s()\\-]", "");
        if (compact.startsWith("+261")) compact = "0" + compact.substring(4);

        // Malagasy mobile networks use national numbers 03X followed by seven digits.
        return compact.matches("03[23478]\\d{7}") ? compact : null;
    }

    static String normalizeAmount(String input) {
        if (input == null || !input.matches("\\d+")) return null;
        String normalized = input.replaceFirst("^0+(?!$)", "");
        return normalized.matches("[1-9]\\d*") ? normalized : null;
    }

    static String buildUssdCode(String recipientNumber, String amount) {
        String normalizedRecipientNumber = normalizeRecipientNumber(recipientNumber);
        String normalizedAmount = normalizeAmount(amount);
        if (normalizedRecipientNumber == null || normalizedAmount == null) {
            throw new IllegalArgumentException("Validated recipient number and amount required");
        }
        return "#111*1*2*" + normalizedRecipientNumber + "*1*" + normalizedAmount + "#";
    }

    static String formatRecipientNumber(String recipientNumber) {
        return recipientNumber.substring(0, 3) + " " + recipientNumber.substring(3, 5)
                + " " + recipientNumber.substring(5, 8) + " " + recipientNumber.substring(8);
    }

    static String formatAmount(String amount) {
        return NumberFormat.getIntegerInstance(Locale.FRANCE).format(new BigInteger(amount)) + " Ar";
    }
}
