package com.example.testapp;

import static org.junit.Assert.assertEquals;

import com.example.testapp.database.SmsMessage;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class SmsStatisticsTest {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @Test public void zeroSmsReturnsNoData() {
        assertEquals(0, SmsStatistics.groupByDate(new ArrayList<>(), UTC).size());
    }

    @Test public void oneSmsHasCountOne() {
        List<SmsStatistics.DailyCount> result = SmsStatistics.groupByDate(
                Arrays.asList(sms(2026, 9, 21, 8)), UTC);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).count);
    }

    @Test public void twoSmsOnSameDayHaveCountTwo() {
        List<SmsStatistics.DailyCount> result = SmsStatistics.groupByDate(
                Arrays.asList(sms(2026, 9, 21, 8), sms(2026, 9, 21, 18)), UTC);
        assertEquals(2, result.get(0).count);
    }

    @Test public void fiveSmsOnSameDayAreGrouped() {
        List<SmsMessage> messages = new ArrayList<>();
        for (int hour = 1; hour <= 5; hour++) messages.add(sms(2026, 9, 21, hour));
        List<SmsStatistics.DailyCount> result = SmsStatistics.groupByDate(messages, UTC);
        assertEquals(1, result.size());
        assertEquals(5, result.get(0).count);
    }

    @Test public void multipleDaysAreChronologicalAndExact() {
        List<SmsMessage> messages = new ArrayList<>();
        add(messages, 20, 21); add(messages, 60, 22); add(messages, 100, 23);
        List<SmsStatistics.DailyCount> result = SmsStatistics.groupByDate(messages, UTC);
        assertEquals(3, result.size());
        assertEquals(20, result.get(0).count);
        assertEquals(60, result.get(1).count);
        assertEquals(100, result.get(2).count);
    }

    @Test public void newlyReceivedSmsUpdatesExistingDay() {
        List<SmsMessage> messages = new ArrayList<>();
        add(messages, 15, 23);
        assertEquals(15, SmsStatistics.groupByDate(messages, UTC).get(0).count);
        messages.add(sms(2026, 9, 23, 23));
        assertEquals(16, SmsStatistics.groupByDate(messages, UTC).get(0).count);
    }

    @Test public void chartScaleUsesReadableStepsWithoutFixedMaximum() {
        assertEquals(2, StatisticsChartView.readableMaximum(2));
        assertEquals(100, StatisticsChartView.readableMaximum(100));
        assertEquals(1_000, StatisticsChartView.readableMaximum(920));
        assertEquals(1_000, StatisticsChartView.readableMaximum(1_000));
        assertEquals(10_000, StatisticsChartView.readableMaximum(10_000));
        assertEquals(150, StatisticsChartView.readableMaximum(120));
        assertEquals(20, StatisticsChartView.readableStep(87));
        assertEquals(2_000, StatisticsChartView.readableStep(10_000));
    }

    @Test public void thirtyDatesRemainThirtyChronologicalCategories() {
        List<SmsMessage> messages = new ArrayList<>();
        for (int day = 1; day <= 30; day++) messages.add(sms(2026, 9, day, 12));
        List<SmsStatistics.DailyCount> result = SmsStatistics.groupByDate(messages, UTC);
        assertEquals(30, result.size());
        for (SmsStatistics.DailyCount count : result) assertEquals(1, count.count);
    }

    @Test public void chartUsesAStableProfessionalBarWidthForEveryDataSetSize() {
        assertEquals(22, StatisticsChartView.BAR_WIDTH_DP);
        assertEquals(48, StatisticsChartView.SLOT_WIDTH_DP);
        assertEquals(48f, StatisticsChartView.contentWidthDp(1), 0f);
        assertEquals(96f, StatisticsChartView.contentWidthDp(2), 0f);
        assertEquals(240f, StatisticsChartView.contentWidthDp(5), 0f);
        assertEquals(336f, StatisticsChartView.contentWidthDp(7), 0f);
        assertEquals(1_440f, StatisticsChartView.contentWidthDp(30), 0f);
    }

    @Test public void chartScrollsOnlyWhenDatesExceedItsViewport() {
        // A 250 dp plotting viewport is representative of a 360 dp Android phone.
        assertEquals(0f, StatisticsChartView.maximumOffsetDp(1, 250), 0f);
        assertEquals(0f, StatisticsChartView.maximumOffsetDp(2, 250), 0f);
        assertEquals(0f, StatisticsChartView.maximumOffsetDp(5, 250), 0f);
        assertEquals(86f, StatisticsChartView.maximumOffsetDp(7, 250), 0f);
        assertEquals(1_190f, StatisticsChartView.maximumOffsetDp(30, 250), 0f);
    }

    private static void add(List<SmsMessage> messages, int count, int day) {
        for (int index = 0; index < count; index++) messages.add(sms(2026, 9, day, index));
    }

    private static SmsMessage sms(int year, int month, int day, int hour) {
        Calendar calendar = Calendar.getInstance(UTC);
        calendar.clear();
        calendar.set(year, month - 1, day, hour, 0);
        return SmsMessage.create("sender", "message", calendar.getTimeInMillis(), true);
    }
}
