package com.netk.mvolatrack.sms;

import java.text.Normalizer;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Central parser for MVola transaction SMS. Add future MVola formats here. */
public final class MvolaMessageParser {
    private static final String AMOUNT = "([0-9]+(?:[ \\u00a0][0-9]+)*)";
    private static final Pattern RECEIVED = Pattern.compile(
            "(?iu)^\\s*" + AMOUNT + "\\s*Ar\\s+re[cç]u\\s+de\\s+(.+?)\\s+"
                    + "((?:\\+261|0)[0-9 \\u00a0]{9,14}?)\\s+le\\s+"
                    + "(\\d{1,2})/(\\d{1,2})/(\\d{2}|\\d{4})\\s+[aà]\\s+"
                    + "(\\d{1,2}):(\\d{2})(?=\\s*\\.|\\s|$)");
    private static final Pattern CREDIT_PURCHASE = Pattern.compile(
            "(?iu)^\\s*achat\\s+de\\s+cr[eé]dit\\s+YAS\\s+r[eé]ussi\\s*:\\s*"
                    + AMOUNT + "\\s*Ar\\s+pour\\s+"
                    + "((?:\\+261|0)[0-9 \\u00a0]{9,14}?)(?=\\s*\\.|\\s+(?:frais|bonus|solde|ref|réf)\\b|\\s*$)");
    private static final Pattern DEPOSIT = Pattern.compile(
            "(?iu)\\bvous\\s+avez\\s+cr[eé]dit[eé]\\s+(.+?)\\s+\\(?\\s*"
                    + "((?:\\+261|0)[0-9 \\u00a0]{9,14}?)(?=\\s*\\)?\\s+de\\b)"
                    + "\\s*\\)?\\s+de\\s+" + AMOUNT + "\\s*Ar\\b");
    private static final Pattern TRANSACTION_DATE = Pattern.compile(
            "(?iu)\\ble\\s+(\\d{1,2})/(\\d{1,2})/(\\d{2}|\\d{4})\\s+[aà]\\s+"
                    + "(\\d{1,2}):(\\d{2})(?=\\s*\\.|\\s|$)");
    private static final Pattern BONUS = field("bonus");
    private static final Pattern FEE = field("frais");
    private static final Pattern BALANCE = Pattern.compile(
            "(?iu)\\bsolde(?:\\s+MVola)?\\s*:\\s*" + AMOUNT + "\\s*Ar\\b");
    private static final Pattern REFERENCE = Pattern.compile(
            "(?iu)\\b(?:ref|réf)\\s*:\\s*([0-9]+)");

    private MvolaMessageParser() {}

    public static ParsedTransaction parse(String rawMessage) {
        return parse(rawMessage, 0L);
    }

    /**
     * Parses a transaction and uses {@code receivedAt} only for formats that do not carry their
     * own business date. The Android sender is deliberately not an input: transaction fields
     * always come from the SMS body.
     */
    public static ParsedTransaction parse(String rawMessage, long receivedAt) {
        if (rawMessage == null) return null;
        String message = Normalizer.normalize(rawMessage, Normalizer.Form.NFKC);
        Matcher match = RECEIVED.matcher(message);
        if (match.find()) return parseReceived(match, message, rawMessage);

        match = DEPOSIT.matcher(message);
        if (match.find()) return parseDeposit(match, message, rawMessage, receivedAt);

        match = CREDIT_PURCHASE.matcher(message);
        if (match.find()) return parseCredit(match, message, rawMessage, receivedAt);
        return null;
    }

    private static ParsedTransaction parseDeposit(Matcher match, String message,
                                                   String rawMessage, long receivedAt) {
        String clientNumber = ClientNumberNormalizer.normalize(match.group(2));
        Long amount = number(match.group(3));
        if (clientNumber == null || amount == null) return null;

        long transactionAt = receivedAt;
        Matcher dateMatch = TRANSACTION_DATE.matcher(message);
        if (dateMatch.find()) {
            Long parsedDate = date(dateMatch.group(1), dateMatch.group(2), dateMatch.group(3),
                    dateMatch.group(4), dateMatch.group(5), TimeZone.getDefault());
            if (parsedDate != null) transactionAt = parsedDate;
        }
        String clientName = match.group(1).trim().replaceAll("\\s+", " ");
        return new ParsedTransaction("Dépôt", clientNumber, clientName, amount,
                text(REFERENCE, message), number(BONUS, message), number(FEE, message),
                number(BALANCE, message), transactionAt, rawMessage);
    }

    private static ParsedTransaction parseReceived(Matcher match, String message,
                                                    String rawMessage) {
        Long amount = number(match.group(1));
        String clientNumber = ClientNumberNormalizer.normalize(match.group(3));
        Long transactionAt = date(match.group(4), match.group(5), match.group(6),
                match.group(7), match.group(8), TimeZone.getDefault());
        if (amount == null || clientNumber == null || transactionAt == null) return null;

        return new ParsedTransaction("Retrait", clientNumber, match.group(2).trim(), amount,
                text(REFERENCE, message), number(BONUS, message), number(FEE, message),
                number(BALANCE, message), transactionAt, rawMessage);
    }

    private static ParsedTransaction parseCredit(Matcher match, String message,
                                                  String rawMessage, long receivedAt) {
        Long amount = number(match.group(1));
        String clientNumber = ClientNumberNormalizer.normalize(match.group(2));
        if (amount == null || clientNumber == null) return null;

        return new ParsedTransaction("Crédit", clientNumber, "-", amount,
                text(REFERENCE, message), number(BONUS, message), number(FEE, message),
                number(BALANCE, message), receivedAt, rawMessage);
    }

    private static Pattern field(String name) {
        return Pattern.compile("(?iu)\\b" + name + "\\s*:\\s*" + AMOUNT + "\\s*Ar\\b");
    }

    private static Long number(Pattern pattern, String message) {
        Matcher matcher = pattern.matcher(message);
        return matcher.find() ? number(matcher.group(1)) : null;
    }

    private static Long number(String value) {
        try {
            return Long.parseLong(value.replaceAll("[ \\u00a0]", ""));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String text(Pattern pattern, String message) {
        Matcher matcher = pattern.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static Long date(String day, String month, String year, String hour, String minute,
                             TimeZone timeZone) {
        try {
            int parsedYear = Integer.parseInt(year);
            if (year.length() == 2) parsedYear += 2000;
            Calendar calendar = Calendar.getInstance(timeZone, Locale.FRENCH);
            calendar.clear();
            calendar.setLenient(false);
            calendar.set(parsedYear, Integer.parseInt(month) - 1, Integer.parseInt(day),
                    Integer.parseInt(hour), Integer.parseInt(minute));
            return calendar.getTimeInMillis();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static final class ParsedTransaction {
        public final String type;
        public final String clientNumber;
        public final String clientName;
        public final long amount;
        public final String reference;
        public final Long bonus;
        public final Long fee;
        public final Long balance;
        public final long transactionAt;
        public final String rawMessage;

        ParsedTransaction(String type, String clientNumber, String clientName, long amount,
                          String reference, Long bonus, Long fee, Long balance,
                          long transactionAt, String rawMessage) {
            this.type = type;
            this.clientNumber = clientNumber;
            this.clientName = clientName;
            this.amount = amount;
            this.reference = reference;
            this.bonus = bonus;
            this.fee = fee;
            this.balance = balance;
            this.transactionAt = transactionAt;
            this.rawMessage = rawMessage;
        }
    }
}
