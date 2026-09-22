package com.example.testapp;

import java.math.BigInteger;

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
        if (input == null || !input.matches("[0-9 ]+")) return null;
        String normalized = input.replace(" ", "").replaceFirst("^0+(?!$)", "");
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
        return formatRecipientInput(recipientNumber);
    }

    static String formatAmount(String amount) {
        return formatAmountInput(amount) + " Ar";
    }

    /** Formats a phone field while it is being edited; the returned value is display-only. */
    static String formatRecipientInput(String input) {
        if (input == null || input.isEmpty()) return "";
        String compact = input.replaceAll("[\\s()\\-]", "");
        if (compact.startsWith("+") && "+261".startsWith(compact)) return compact;
        if (compact.startsWith("+261")) compact = "0" + compact.substring(4);
        String digits = compact.replaceAll("\\D", "");
        if (digits.length() > 10) digits = digits.substring(0, 10);
        return groupDigits(digits, new int[]{3, 2, 3, 2});
    }

    /** Formats an amount with non-breaking-independent space grouping for an editable field. */
    static String formatAmountInput(String input) {
        if (input == null) return "";
        String digits = input.replaceAll("\\D", "");
        if (digits.isEmpty()) return "";
        // BigInteger also removes insignificant leading zeroes without numeric overflow.
        String normalized = new BigInteger(digits).toString();
        StringBuilder display = new StringBuilder(normalized);
        for (int index = display.length() - 3; index > 0; index -= 3) {
            display.insert(index, ' ');
        }
        return display.toString();
    }

    private static String groupDigits(String digits, int[] groups) {
        StringBuilder display = new StringBuilder();
        int offset = 0;
        for (int size : groups) {
            if (offset >= digits.length()) break;
            if (display.length() > 0) display.append(' ');
            int end = Math.min(offset + size, digits.length());
            display.append(digits, offset, end);
            offset = end;
        }
        return display.toString();
    }
}
