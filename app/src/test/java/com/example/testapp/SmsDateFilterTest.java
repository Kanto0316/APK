package com.example.testapp;

import static org.junit.Assert.assertEquals;

import com.example.testapp.database.SmsMessage;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class SmsDateFilterTest {
    private static final TimeZone ZONE = TimeZone.getTimeZone("Europe/Paris");

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

    private static SmsMessage message(int year, int month, int day, int hour) {
        return SmsMessage.create("sender", "body", time(year, month, day, hour), true);
    }

    private static long time(int year, int month, int day, int hour) {
        Calendar calendar = Calendar.getInstance(ZONE);
        calendar.clear();
        calendar.set(year, month - 1, day, hour, 0, 0);
        return calendar.getTimeInMillis();
    }
}
