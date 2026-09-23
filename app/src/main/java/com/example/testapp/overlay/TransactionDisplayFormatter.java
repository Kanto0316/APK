package com.example.testapp.overlay;

import java.text.NumberFormat;
import java.util.Locale;

/** Null-safe formatting shared by the in-app dialog, system overlay and notification. */
public final class TransactionDisplayFormatter {
    private TransactionDisplayFormatter() {}

    public static String text(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    public static String phone(String value) {
        String normalized = text(value);
        if ("-".equals(normalized)) return normalized;
        String digits = normalized.replaceAll("\\D", "");
        if (digits.length() == 10 && digits.startsWith("0")) {
            return digits.substring(0, 3) + " " + digits.substring(3, 5) + " "
                    + digits.substring(5, 8) + " " + digits.substring(8);
        }
        return normalized;
    }

    public static String amount(long value) {
        return "+ " + number(value) + " Ar";
    }

    /** Credit purchases and deposits are displayed as values, without a direction prefix. */
    public static String amount(String type, long value) {
        String transactionType = text(type);
        return "Crédit".equalsIgnoreCase(transactionType)
                || "Dépôt".equalsIgnoreCase(transactionType)
                ? number(value) + " Ar" : amount(value);
    }

    public static String balance(Long value) {
        return value == null ? "-" : number(value) + " Ar";
    }

    private static String number(long value) {
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.FRENCH);
        format.setGroupingUsed(true);
        return format.format(value).replace('\u00a0', ' ').replace('\u202f', ' ');
    }
}
