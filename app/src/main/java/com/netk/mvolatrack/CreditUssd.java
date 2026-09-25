package com.netk.mvolatrack;

import java.math.BigInteger;

/** Pure validation and USSD helpers for the credit purchase workflow. */
final class CreditUssd {
    static final long MIN_AMOUNT = 100L;
    static final long MAX_AMOUNT = 500_000L;

    private CreditUssd() {}

    static long resolveAmount(String input) {
        String normalized = DepositUssd.normalizeAmount(input);
        if (normalized == null) return -1L;
        if ("1".equals(normalized)) return 500L;
        if ("2".equals(normalized)) return 1_000L;
        try {
            long amount = Long.parseLong(normalized);
            return amount >= MIN_AMOUNT && amount <= MAX_AMOUNT ? amount : -1L;
        } catch (NumberFormatException error) {
            return -1L;
        }
    }

    static String formatBoundedAmountInput(String input, String lastValidDisplay) {
        String display = DepositUssd.formatAmountInput(input);
        if (display.isEmpty()) return display;
        String normalized = display.replace(" ", "");
        return new BigInteger(normalized).compareTo(BigInteger.valueOf(MAX_AMOUNT)) <= 0
                ? display : lastValidDisplay;
    }

    static String buildUssdCode(String recipientNumber, long amount) {
        String recipient = DepositUssd.normalizeRecipientNumber(recipientNumber);
        if (recipient == null || amount < MIN_AMOUNT || amount > MAX_AMOUNT) {
            throw new IllegalArgumentException("Validated recipient number and credit required");
        }
        return "#111*1*4*2*1*" + recipient + "*" + amount + "#";
    }
}
