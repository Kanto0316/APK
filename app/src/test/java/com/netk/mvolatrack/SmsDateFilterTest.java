package com.netk.mvolatrack;

import static org.junit.Assert.assertEquals;

import com.netk.mvolatrack.database.SmsMessage;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class SmsDateFilterTest {
    private static final TimeZone ZONE = TimeZone.getTimeZone("Europe/Paris");

    @Test
    public void unparsedMvolaIsStoredButExcludedFromEveryDisplayFilter() {
        SmsMessage raw = SmsMessage.create("MVola", "format pas encore reconnu",
                time(2026, 9, 22, 10), true);
        SmsMessage parsed = message("0343242318", 2026, 9, 22, 9);

        List<SmsDateFilter.DisplayMessage> result = SmsDateFilter.apply(
                Arrays.asList(raw, parsed), SmsDateFilter.Period.ALL, null,
                time(2026, 9, 22, 12), ZONE);

        assertEquals(1, result.size());
        assertEquals(parsed, result.get(0).message);
        assertEquals("format pas encore reconnu", raw.messageBody);
        assertEquals("MVola", raw.sender);
    }

    @Test
    public void allPreservesEveryMessageAndOriginalNumbers() {
        List<SmsMessage> source = Arrays.asList(message(2027, 1, 1, 12),
                message(2026, 12, 31, 12), message(2026, 12, 20, 12));

        List<SmsDateFilter.DisplayMessage> result = SmsDateFilter.apply(source,
                SmsDateFilter.Period.ALL, null, time(2027, 1, 1, 15), ZONE);

        assertEquals(3, result.size());
        assertEquals(3, result.get(0).originalNumber);
        assertEquals(2, result.get(1).originalNumber);
        assertEquals(1, result.get(2).originalNumber);
    }

    @Test
    public void todayAndYesterdayUseLocalCalendarDaysAcrossYearBoundary() {
        List<SmsMessage> source = Arrays.asList(message(2027, 1, 1, 0),
                message(2026, 12, 31, 23), message(2026, 12, 30, 23));

        assertEquals(1, SmsDateFilter.apply(source, SmsDateFilter.Period.TODAY, null,
                time(2027, 1, 1, 8), ZONE).size());
        List<SmsDateFilter.DisplayMessage> yesterday = SmsDateFilter.apply(source,
                SmsDateFilter.Period.YESTERDAY, null, time(2027, 1, 1, 8), ZONE);
        assertEquals(1, yesterday.size());
        assertEquals(2, yesterday.get(0).originalNumber);
    }

    @Test
    public void rollingPeriodsIncludeTodayAndExpectedPreviousCalendarDays() {
        List<SmsMessage> source = Arrays.asList(message(2026, 9, 22, 23),
                message(2026, 9, 16, 0), message(2026, 9, 15, 23),
                message(2026, 8, 24, 0), message(2026, 8, 23, 23));

        List<SmsDateFilter.DisplayMessage> seven = SmsDateFilter.apply(source,
                SmsDateFilter.Period.SEVEN_DAYS, null, time(2026, 9, 22, 12), ZONE);
        assertEquals(2, seven.size());
        assertEquals(5, seven.get(0).originalNumber);
        assertEquals(4, seven.get(1).originalNumber);
        assertEquals(4, SmsDateFilter.apply(source, SmsDateFilter.Period.THIRTY_DAYS,
                null, time(2026, 9, 22, 12), ZONE).size());
    }

    @Test
    public void customDateUsesSelectedLocalDayAndKeepsSparseNumber() {
        List<SmsMessage> source = Arrays.asList(message(2026, 9, 22, 10),
                message(2026, 9, 15, 20), message(2026, 9, 14, 10));

        List<SmsDateFilter.DisplayMessage> result = SmsDateFilter.apply(source,
                SmsDateFilter.Period.CUSTOM_DATE, time(2026, 9, 15, 0),
                time(2026, 9, 22, 12), ZONE);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).originalNumber);
    }

    @Test
    public void numberSearchIgnoresSpacesAndPreservesLeadingZero() {
        List<SmsMessage> source = Arrays.asList(
                message("+261384900237", 2026, 9, 22, 10),
                message("0344153878", 2026, 9, 22, 9));

        for (String query : Arrays.asList("03849", "038 49", "03849002", "038 49 002 37")) {
            List<SmsDateFilter.DisplayMessage> result = SmsDateFilter.apply(source,
                    SmsDateFilter.Period.ALL, null, time(2026, 9, 22, 12), ZONE, query);
            assertEquals(query, 1, result.size());
            assertEquals("+261384900237", result.get(0).message.sender);
            assertEquals(2, result.get(0).originalNumber);
        }
    }

    @Test
    public void numberSearchCombinesWithDateFilterAndEmptyQueryRestoresPeriod() {
        List<SmsMessage> source = Arrays.asList(
                message("0384900237", 2026, 9, 22, 10),
                message("038 49 111 11", 2026, 9, 21, 10),
                message("0344153878", 2026, 9, 22, 9));

        List<SmsDateFilter.DisplayMessage> matchingToday = SmsDateFilter.apply(source,
                SmsDateFilter.Period.TODAY, null, time(2026, 9, 22, 12), ZONE, "038 49");
        assertEquals(1, matchingToday.size());
        assertEquals("0384900237", matchingToday.get(0).message.sender);

        List<SmsDateFilter.DisplayMessage> restoredToday = SmsDateFilter.apply(source,
                SmsDateFilter.Period.TODAY, null, time(2026, 9, 22, 12), ZONE, "");
        assertEquals(2, restoredToday.size());
    }

    @Test
    public void nameSearchIsPartialCaseAndAccentInsensitive() {
        List<SmsMessage> source = Arrays.asList(
                namedMessage("Ravaka", "0385829562", 2026, 9, 22, 10),
                namedMessage("Falitiana", "0345829482", 2026, 9, 22, 9),
                namedMessage("MARIÉTTE", "0344153878", 2026, 9, 22, 8));

        assertSingleNamedResult(source, "rav", "Ravaka");
        assertSingleNamedResult(source, "RAVAKA", "Ravaka");
        assertSingleNamedResult(source, "fali", "Falitiana");
        assertSingleNamedResult(source, "mariette", "MARIÉTTE");
    }

    @Test
    public void nameSearchCombinesWithDateAndDoesNotSearchRawSmsFields() {
        List<SmsMessage> source = Arrays.asList(
                namedMessage("Ravaka", "0385829562", 2026, 9, 22, 10),
                namedMessage("Ravaka", "0345829482", 2026, 9, 21, 10),
                namedMessage("Falitiana", "0344153878", 2026, 9, 22, 9));

        List<SmsDateFilter.DisplayMessage> today = SmsDateFilter.apply(source,
                SmsDateFilter.Period.TODAY, null, time(2026, 9, 22, 12), ZONE, "ravaka");
        assertEquals(1, today.size());
        assertEquals("0385829562", today.get(0).message.sender);
        assertEquals(0, SmsDateFilter.apply(source, SmsDateFilter.Period.ALL, null,
                time(2026, 9, 22, 12), ZONE, "solde").size());
    }

    @Test
    public void numberSearchRemovesSeparators() {
        List<SmsMessage> source = Arrays.asList(
                namedMessage("Ravaka", "0385829562", 2026, 9, 22, 10));

        assertEquals(1, SmsDateFilter.apply(source, SmsDateFilter.Period.ALL, null,
                time(2026, 9, 22, 12), ZONE, "038-58").size());
        assertEquals("0385829562", SmsDateFilter.normalizeNumber("038 58-295.62"));
    }

    @Test
    public void todayAndWithdrawalFiltersAreCombined() {
        List<SmsMessage> source = Arrays.asList(
                typedMessage("Retrait", "Ravaka", "0385829562", 2026, 9, 22, 10),
                typedMessage("Dépôt", "Ravaka", "0385829562", 2026, 9, 22, 9),
                typedMessage("Retrait", "Ravaka", "0385829562", 2026, 9, 21, 10));

        assertSingleTypeResult(source, SmsDateFilter.Period.TODAY, "",
                SmsDateFilter.TransactionType.WITHDRAWAL, "Retrait");
    }

    @Test
    public void allDatesAndDepositFiltersAreCombined() {
        List<SmsMessage> source = Arrays.asList(
                typedMessage("Dépôt", "Ravaka", "0385829562", 2026, 8, 1, 10),
                typedMessage("Crédit", "-", "0386825677", 2026, 9, 22, 9));

        assertSingleTypeResult(source, SmsDateFilter.Period.ALL, "",
                SmsDateFilter.TransactionType.DEPOSIT, "Dépôt");
    }

    @Test
    public void sevenDaysAndCreditFiltersAreCombined() {
        List<SmsMessage> source = Arrays.asList(
                typedMessage("Crédit", "-", "0386825677", 2026, 9, 16, 10),
                typedMessage("Crédit", "-", "0386825677", 2026, 9, 15, 10),
                typedMessage("Retrait", "Ravaka", "0385829562", 2026, 9, 22, 9));

        assertSingleTypeResult(source, SmsDateFilter.Period.SEVEN_DAYS, "",
                SmsDateFilter.TransactionType.CREDIT, "Crédit");
    }

    @Test
    public void nameSearchAndWithdrawalFiltersAreCombined() {
        List<SmsMessage> source = Arrays.asList(
                typedMessage("Retrait", "Ravaka", "0385829562", 2026, 9, 22, 10),
                typedMessage("Dépôt", "Ravaka", "0340677891", 2026, 9, 22, 9),
                typedMessage("Retrait", "Falitiana", "0345829482", 2026, 9, 22, 8));

        assertSingleTypeResult(source, SmsDateFilter.Period.ALL, "ravaka",
                SmsDateFilter.TransactionType.WITHDRAWAL, "Retrait");
    }

    @Test
    public void numberSearchAndCreditFiltersAreCombined() {
        List<SmsMessage> source = Arrays.asList(
                typedMessage("Crédit", "-", "0386825677", 2026, 9, 22, 10),
                typedMessage("Retrait", "Ravaka", "0386825677", 2026, 9, 22, 9),
                typedMessage("Crédit", "-", "0341444033", 2026, 9, 22, 8));

        assertSingleTypeResult(source, SmsDateFilter.Period.ALL, "03868",
                SmsDateFilter.TransactionType.CREDIT, "Crédit");
    }

    @Test
    public void allTypesAndYesterdayPreserveEveryRecognizedType() {
        List<SmsMessage> source = Arrays.asList(
                typedMessage("Dépôt", "Ravaka", "0385829562", 2026, 9, 21, 10),
                typedMessage("Retrait", "Falitiana", "0345829482", 2026, 9, 21, 9),
                typedMessage("Crédit", "-", "0386825677", 2026, 9, 22, 8));

        List<SmsDateFilter.DisplayMessage> result = SmsDateFilter.apply(source,
                SmsDateFilter.Period.YESTERDAY, null, time(2026, 9, 22, 12), ZONE, "",
                SmsDateFilter.TransactionType.ALL);
        assertEquals(2, result.size());
    }

    private static void assertSingleTypeResult(List<SmsMessage> source,
                                               SmsDateFilter.Period period, String query,
                                               SmsDateFilter.TransactionType type,
                                               String expectedParsedType) {
        List<SmsDateFilter.DisplayMessage> result = SmsDateFilter.apply(source, period, null,
                time(2026, 9, 22, 12), ZONE, query, type);
        assertEquals(1, result.size());
        assertEquals(expectedParsedType, com.netk.mvolatrack.sms.MvolaMessageParser.parse(
                result.get(0).message.messageBody, result.get(0).message.receivedDate).type);
    }

    private static void assertSingleNamedResult(List<SmsMessage> source, String query,
                                                String expectedName) {
        List<SmsDateFilter.DisplayMessage> result = SmsDateFilter.apply(source,
                SmsDateFilter.Period.ALL, null, time(2026, 9, 22, 12), ZONE, query);
        assertEquals(query, 1, result.size());
        assertEquals(expectedName,
                com.netk.mvolatrack.sms.MvolaMessageParser.parse(
                        result.get(0).message.messageBody).clientName);
    }

    private static SmsMessage message(int year, int month, int day, int hour) {
        return message("sender", year, month, day, hour);
    }

    private static SmsMessage message(String sender, int year, int month, int day, int hour) {
        String number = sender.replaceAll("[^0-9+]", "");
        if (!(number.startsWith("0") || number.startsWith("+261"))) number = "0343242318";
        String body = "1 000 Ar recu de Client " + number + " le "
                + String.format(java.util.Locale.ROOT, "%02d/%02d/%02d a %02d:00. ",
                day, month, year % 100, hour)
                + "Bonus:1 Ar. Solde: 10 000 Ar. Ref: 123456.";
        return SmsMessage.create(sender, body, time(year, month, day, hour), true);
    }

    private static SmsMessage namedMessage(String name, String number, int year, int month,
                                           int day, int hour) {
        String body = "1 000 Ar recu de " + name + " " + number + " le "
                + String.format(java.util.Locale.ROOT, "%02d/%02d/%02d a %02d:00. ",
                day, month, year % 100, hour)
                + "Bonus:1 Ar. Solde: 10 000 Ar. Ref: 123456.";
        return SmsMessage.create(number, body, time(year, month, day, hour), true);
    }

    private static SmsMessage typedMessage(String type, String name, String number, int year,
                                           int month, int day, int hour) {
        String date = String.format(java.util.Locale.ROOT, "%02d/%02d/%02d a %02d:00",
                day, month, year % 100, hour);
        String body;
        if ("Dépôt".equals(type)) {
            body = "Vous avez credite " + name + " (" + number + ") de 1 000 Ar le " + date
                    + ". Bonus:1 Ar. Solde : 10 000 Ar. Ref: 123456";
        } else if ("Crédit".equals(type)) {
            body = "Achat de credit YAS reussi: 500 Ar pour " + number
                    + ". Frais: 0 Ar. Bonus:24 Ar. Solde MVola : 10 000 Ar. Ref: 123456";
        } else {
            body = "1 000 Ar recu de " + name + " " + number + " le " + date
                    + ". Bonus:1 Ar. Solde: 10 000 Ar. Ref: 123456.";
        }
        return SmsMessage.create(number, body, time(year, month, day, hour), true);
    }

    private static long time(int year, int month, int day, int hour) {
        Calendar calendar = Calendar.getInstance(ZONE);
        calendar.clear();
        calendar.set(year, month - 1, day, hour, 0, 0);
        return calendar.getTimeInMillis();
    }
}
