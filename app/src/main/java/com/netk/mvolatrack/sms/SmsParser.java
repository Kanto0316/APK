package com.netk.mvolatrack.sms;

import com.netk.mvolatrack.database.Transaction;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts supported financial SMS messages into locally persisted transactions. */
public class SmsParser {
    private static final String NUMBER = "([0-9]+(?:[ \\u00A0.,][0-9]+)*)";
    private static final String CURRENCY = "(MGA|Ar)";
    private static final Pattern KEYWORD = Pattern.compile(
            "(?i)\\b(SOLDE|BONUS|RECHARGE|PAIEMENT|TRANSACTION)\\b");

    public Transaction parse(String message, String sender, long date) {
        if (message == null || message.trim().isEmpty()) return null;
        String normalized = Normalizer.normalize(message, Normalizer.Form.NFKC);
        Matcher typeMatcher = KEYWORD.matcher(normalized);
        if (!typeMatcher.find()) return null;

        String type = typeMatcher.group(1).toUpperCase(Locale.ROOT);
        Amount primary = findAmountForKeyword(normalized, type);
        if (primary == null) return null;

        Amount balanceAmount = findAmountForKeyword(normalized, "SOLDE");
        Amount bonusAmount = findAmountForKeyword(normalized, "BONUS");
        Double balance = balanceAmount == null ? null : balanceAmount.value;
        Double bonus = bonusAmount == null ? null : bonusAmount.value;

        return new Transaction(date, sender == null ? "Inconnu" : sender, type, primary.value,
                primary.currency, balance, bonus, message);
    }

    private Amount findAmountForKeyword(String message, String keyword) {
        String separator = "(?:\\s|:|=|est|de|du|crédité|credite){0,30}";
        Pattern after = Pattern.compile("(?i)\\b" + keyword + "\\b" + separator
                + NUMBER + "\\s*" + CURRENCY);
        Matcher matcher = after.matcher(message);
        if (matcher.find()) return toAmount(matcher.group(1), matcher.group(2));

        Pattern currencyFirst = Pattern.compile("(?i)\\b" + keyword + "\\b" + separator
                + CURRENCY + "\\s*" + NUMBER);
        matcher = currencyFirst.matcher(message);
        if (matcher.find()) return toAmount(matcher.group(2), matcher.group(1));
        return null;
    }

    private Amount toAmount(String raw, String currency) {
        String compact = raw.replace(" ", "").replace("\u00A0", "");
        int lastComma = compact.lastIndexOf(',');
        int lastDot = compact.lastIndexOf('.');
        int decimalPosition = Math.max(lastComma, lastDot);
        if (decimalPosition >= 0 && compact.length() - decimalPosition - 1 <= 2) {
            compact = compact.substring(0, decimalPosition).replace(",", "").replace(".", "")
                    + "." + compact.substring(decimalPosition + 1);
        } else {
            compact = compact.replace(",", "").replace(".", "");
        }
        try {
            String standardCurrency = currency.equalsIgnoreCase("MGA") ? "MGA" : "Ar";
            return new Amount(Double.parseDouble(compact), standardCurrency);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static class Amount {
        final double value;
        final String currency;
        Amount(double value, String currency) { this.value = value; this.currency = currency; }
    }
}
