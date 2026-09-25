package com.netk.mvolatrack;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure display transformations used by the SMS list. */
final class SmsDisplayFormatter {
    private static final Pattern MALAGASY_INTERNATIONAL_NUMBER =
            Pattern.compile("^\\+261(\\d{2})(\\d{2})(\\d{3})(\\d{2})$");
    private static final Pattern MALAGASY_LOCAL_NUMBER =
            Pattern.compile("^0(\\d{2})(\\d{2})(\\d{3})(\\d{2})$");

    private SmsDisplayFormatter() {}

    static int listNumber(int totalSmsCount, int adapterPosition) {
        return totalSmsCount - adapterPosition;
    }

    static String sender(String originalSender) {
        if (originalSender == null) return null;

        Matcher matcher = MALAGASY_INTERNATIONAL_NUMBER.matcher(originalSender);
        if (!matcher.matches()) matcher = MALAGASY_LOCAL_NUMBER.matcher(originalSender);
        if (!matcher.matches()) return originalSender;

        return "0" + matcher.group(1) + " " + matcher.group(2) + " "
                + matcher.group(3) + " " + matcher.group(4);
    }
}
