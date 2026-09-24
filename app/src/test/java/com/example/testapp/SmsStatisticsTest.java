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

    @Test public void unparsedSmsDoesNotAppearInTransactionChart() {
        SmsMessage raw = SmsMessage.create("MVola", "message non reconnu", 1L, true);
        assertEquals(0, SmsStatistics.groupByDate(Arrays.asList(raw), UTC).size());
        assertEquals(false, SmsStatistics.hasActivity(SmsStatistics.forMonth(
                Arrays.asList(raw), 2026, Calendar.SEPTEMBER, UTC)));
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

    @Test public void chartIdentifiesTodayByCompleteLocalDate() {
        long today = utcDate(2026, Calendar.SEPTEMBER, 24, 12);

        assertEquals(true, StatisticsChartView.isSameLocalDay(
                utcDate(2026, Calendar.SEPTEMBER, 24, 1), today, UTC));
        assertEquals(false, StatisticsChartView.isSameLocalDay(
                utcDate(2026, Calendar.SEPTEMBER, 23, 23), today, UTC));
        assertEquals(false, StatisticsChartView.isSameLocalDay(
                utcDate(2025, Calendar.SEPTEMBER, 24, 12), today, UTC));
    }

    @Test public void chartUsesTheDeviceTimeZoneToIdentifyToday() {
        TimeZone antananarivo = TimeZone.getTimeZone("Indian/Antananarivo");
        long lateUtcSeptember23 = utcDate(2026, Calendar.SEPTEMBER, 23, 22);
        long earlyUtcSeptember24 = utcDate(2026, Calendar.SEPTEMBER, 24, 1);

        assertEquals(true, StatisticsChartView.isSameLocalDay(
                lateUtcSeptember23, earlyUtcSeptember24, antananarivo));
        assertEquals(false, StatisticsChartView.isSameLocalDay(
                lateUtcSeptember23, earlyUtcSeptember24, UTC));
    }

    @Test public void selectedMonthFiltersBothMonthAndYearAndFillsMissingDays() {
        List<SmsMessage> messages = Arrays.asList(
                sms(2026, 9, 21, 8), sms(2026, 9, 21, 18),
                sms(2026, 9, 22, 8), sms(2026, 10, 3, 8), sms(2027, 9, 21, 8));
        List<SmsStatistics.DailyCount> september =
                SmsStatistics.forMonth(messages, 2026, Calendar.SEPTEMBER, UTC);
        assertEquals(30, september.size());
        assertEquals(0, september.get(19).count);
        assertEquals(2, september.get(20).count);
        assertEquals(1, september.get(21).count);
        assertEquals(0, september.get(22).count);
    }

    @Test public void calendarMonthHasCorrectLengthIncludingLeapYears() {
        assertEquals(28, SmsStatistics.forMonth(new ArrayList<>(), 2026,
                Calendar.FEBRUARY, UTC).size());
        assertEquals(29, SmsStatistics.forMonth(new ArrayList<>(), 2028,
                Calendar.FEBRUARY, UTC).size());
        assertEquals(30, SmsStatistics.forMonth(new ArrayList<>(), 2026,
                Calendar.APRIL, UTC).size());
        assertEquals(31, SmsStatistics.forMonth(new ArrayList<>(), 2026,
                Calendar.DECEMBER, UTC).size());
    }

    @Test public void emptyCalendarMonthIsReportedWithoutLosingItsDays() {
        List<SmsStatistics.DailyCount> month = SmsStatistics.forMonth(
                Arrays.asList(sms(2026, 9, 21, 8)), 2026, Calendar.OCTOBER, UTC);
        assertEquals(31, month.size());
        assertEquals(false, SmsStatistics.hasActivity(month));
    }

    @Test public void adjacentMonthsAcrossYearBoundaryRemainIndependent() {
        List<SmsMessage> messages = Arrays.asList(
                sms(2026, 12, 31, 23), sms(2027, 1, 1, 1));
        List<SmsStatistics.DailyCount> december = SmsStatistics.forMonth(
                messages, 2026, Calendar.DECEMBER, UTC);
        List<SmsStatistics.DailyCount> january = SmsStatistics.forMonth(
                messages, 2027, Calendar.JANUARY, UTC);
        assertEquals(1, december.get(30).count);
        assertEquals(1, january.get(0).count);
    }

    @Test public void mvolaSummaryUsesTransactionDateDistinctClientsAndExtractedBonus() {
        Calendar now = Calendar.getInstance(UTC);
        now.clear();
        now.set(2026, Calendar.SEPTEMBER, 21, 12, 0);
        List<SmsMessage> transactions = Arrays.asList(
                mvola("0381453472", "08:31", 250, 1L),
                mvola("0381453472", "09:31", 50, 2L),
                mvola("0343242318", "08:19", 58, 3L),
                SmsMessage.create("MVola", "message non reconnu", now.getTimeInMillis(), true));

        SmsStatistics.TransactionSummary summary = SmsStatistics.summarize(transactions,
                now.getTimeInMillis(), 2026, Calendar.SEPTEMBER, UTC);

        assertEquals(3, summary.todayTransactions);
        assertEquals(358, summary.todayBonus);
        assertEquals(2, summary.todayClients.size());
        assertEquals(3, summary.monthTransactions);
        assertEquals(2, summary.monthClients.size());
    }

    @Test public void bonusChartUsesParsedTransactionDateAndMatchesSelectedMonthTotal() {
        Calendar now = Calendar.getInstance(UTC);
        now.clear();
        now.set(2026, Calendar.SEPTEMBER, 23, 12, 0);
        SmsMessage septemberBonus = SmsMessage.create("Autre expéditeur",
                "3 000 Ar recu de LOVASOA 0343242318 le 21/09/26 a 08:19. "
                        + "Bonus:58 Ar. Solde: 82 023 Ar. Ref: 7582999265.",
                // Deliberately received in October: transactionAt must win.
                utcDate(2026, Calendar.OCTOBER, 1, 9), true);

        SmsStatistics.TransactionSummary september = SmsStatistics.summarize(
                Arrays.asList(septemberBonus), now.getTimeInMillis(),
                2026, Calendar.SEPTEMBER, UTC);
        SmsStatistics.TransactionSummary september2025 = SmsStatistics.summarize(
                Arrays.asList(septemberBonus), now.getTimeInMillis(),
                2025, Calendar.SEPTEMBER, UTC);

        assertEquals(58, september.monthBonus);
        assertEquals(1, september.monthlyBonus.size());
        assertEquals(58, september.monthlyBonus.get(0).count);
        Calendar barDate = Calendar.getInstance(UTC);
        barDate.setTimeInMillis(september.monthlyBonus.get(0).localDayTimestamp);
        assertEquals(2026, barDate.get(Calendar.YEAR));
        assertEquals(Calendar.SEPTEMBER, barDate.get(Calendar.MONTH));
        assertEquals(21, barDate.get(Calendar.DAY_OF_MONTH));
        assertEquals(september.monthBonus, SmsStatistics.sum(september.monthlyBonus));
        assertEquals(0, september2025.monthBonus);
        assertEquals(0, september2025.monthlyBonus.size());
    }

    @Test public void transactionChartUsesSummarySourceAndGroupsSelectedMonthByDay() {
        Calendar now = Calendar.getInstance(UTC);
        now.clear();
        now.set(2026, Calendar.SEPTEMBER, 23, 12, 0);
        List<SmsMessage> messages = Arrays.asList(
                mvolaOnDay("0343242318", 21, "08:19"),
                mvolaOnDay("0381453472", 21, "09:19"),
                mvolaOnDay("0331234567", 21, "10:19"),
                mvolaOnDay("0321234567", 22, "08:19"));

        SmsStatistics.TransactionSummary summary = SmsStatistics.summarize(messages,
                now.getTimeInMillis(), 2026, Calendar.SEPTEMBER, UTC);

        assertEquals(4, summary.monthTransactions);
        assertEquals(2, summary.monthlyTransactions.size());
        assertEquals(3, summary.monthlyTransactions.get(0).count);
        assertEquals(1, summary.monthlyTransactions.get(1).count);
        assertEquals(4, SmsStatistics.sum(summary.monthlyTransactions));
    }

    @Test public void singleParsedTransactionProducesVisibleSeptemberBar() {
        Calendar now = Calendar.getInstance(UTC);
        now.clear();
        now.set(2026, Calendar.SEPTEMBER, 23, 12, 0);

        SmsStatistics.TransactionSummary summary = SmsStatistics.summarize(
                Arrays.asList(mvolaOnDay("0343242318", 21, "08:19")),
                now.getTimeInMillis(), 2026, Calendar.SEPTEMBER, UTC);

        assertEquals(1, summary.monthTransactions);
        assertEquals(1, summary.monthlyTransactions.size());
        assertEquals(1, summary.monthlyTransactions.get(0).count);
        Calendar barDate = Calendar.getInstance(UTC);
        barDate.setTimeInMillis(summary.monthlyTransactions.get(0).localDayTimestamp);
        assertEquals(21, barDate.get(Calendar.DAY_OF_MONTH));
    }

    @Test public void bonusChartSumsBonusesOnTheSameBusinessDay() {
        Calendar now = Calendar.getInstance(UTC);
        now.clear();
        now.set(2026, Calendar.SEPTEMBER, 23, 12, 0);
        List<SmsMessage> messages = Arrays.asList(
                mvola("0343242318", "08:19", 58, 1L),
                mvola("0381453472", "09:31", 250, 2L));

        SmsStatistics.TransactionSummary summary = SmsStatistics.summarize(messages,
                now.getTimeInMillis(), 2026, Calendar.SEPTEMBER, UTC);

        assertEquals(1, summary.monthlyBonus.size());
        assertEquals(308, summary.monthlyBonus.get(0).count);
        assertEquals(summary.monthBonus, SmsStatistics.sum(summary.monthlyBonus));
    }

    @Test public void userChartCountsDistinctNormalizedClientsPerBusinessDay() {
        Calendar now = Calendar.getInstance(UTC);
        now.clear();
        now.set(2026, Calendar.SEPTEMBER, 23, 12, 0);
        List<SmsMessage> messages = Arrays.asList(
                mvola("0343242318", "08:19", 1, utcDate(2026, Calendar.OCTOBER, 1, 9)),
                mvola("034 32 423 18", "09:19", 1, 2L),
                mvola("+261343242318", "10:19", 1, 3L),
                mvola("0381453472", "11:19", 1, 4L),
                SmsMessage.create("MVola", "message non reconnu", 5L, true));

        SmsStatistics.TransactionSummary summary = SmsStatistics.summarize(messages,
                now.getTimeInMillis(), 2026, Calendar.SEPTEMBER, UTC);

        assertEquals(2, summary.monthClients.size());
        assertEquals(1, summary.monthlyUsers.size());
        assertEquals(2, summary.monthlyUsers.get(0).count);
        Calendar barDate = Calendar.getInstance(UTC);
        barDate.setTimeInMillis(summary.monthlyUsers.get(0).localDayTimestamp);
        assertEquals(21, barDate.get(Calendar.DAY_OF_MONTH));
    }

    @Test public void monthlyUserTotalAndDailyBarsUseTheirRequiredDistinctScopes() {
        Calendar now = Calendar.getInstance(UTC);
        now.clear();
        now.set(2026, Calendar.SEPTEMBER, 23, 12, 0);
        List<SmsMessage> messages = Arrays.asList(
                mvolaOnDay("0343242318", 20, "08:19"),
                mvolaOnDay("0343242318", 21, "08:19"),
                mvolaOnDay("0381453472", 21, "09:19"));

        SmsStatistics.TransactionSummary summary = SmsStatistics.summarize(messages,
                now.getTimeInMillis(), 2026, Calendar.SEPTEMBER, UTC);

        assertEquals(2, summary.monthClients.size());
        assertEquals(2, summary.monthlyUsers.size());
        assertEquals(1, summary.monthlyUsers.get(0).count);
        assertEquals(2, summary.monthlyUsers.get(1).count);
    }

    @Test public void todayTypeCountersUseParsedTransactionsOnly() {
        long now = utcDate(2026, Calendar.SEPTEMBER, 23, 12);
        long yesterday = utcDate(2026, Calendar.SEPTEMBER, 22, 12);
        List<SmsMessage> messages = Arrays.asList(
                deposit("ALICE", "0340000001", 1000, "1001", now),
                deposit("BRUNO", "0340000002", 2000, "1002", now),
                credit("0340000003", 500, "1003", now),
                withdrawal("0340000004", 100, "1004", 23, now),
                withdrawal("0340000005", 200, "1005", 23, now),
                withdrawal("0340000006", 300, "1006", 23, now),
                SmsMessage.create("MVola", "SMS ordinaire non parsé", now, true),
                deposit("HIER", "0340000007", 4000, "1007", yesterday));

        SmsStatistics.TransactionSummary summary = SmsStatistics.summarize(
                messages, now, 2026, Calendar.SEPTEMBER, UTC);

        assertEquals(6L, summary.todayTransactions);
        assertEquals(2L, summary.todayDepositTransactions);
        assertEquals(1L, summary.todayCreditTransactions);
        assertEquals(3L, summary.todayWithdrawalTransactions);
        assertEquals(summary.todayTransactions, summary.todayDepositTransactions
                + summary.todayCreditTransactions + summary.todayWithdrawalTransactions);
    }

    @Test public void creditIncrementsCreditCounterAndContributesItsParsedBonus() {
        long receivedAt = utcDate(2026, Calendar.SEPTEMBER, 23, 10);
        SmsMessage credit = SmsMessage.create("MVola",
                "Achat de crédit YAS réussi : 500 Ar pour 0386825677. Frais: 0 Ar. "
                        + "Bonus:24 Ar. Solde MVola : 80 869 Ar. Ref: 7581687806",
                receivedAt, true);

        SmsStatistics.TransactionSummary summary = SmsStatistics.summarize(
                Arrays.asList(credit), receivedAt, 2026, Calendar.SEPTEMBER, UTC);

        assertEquals(1L, summary.todayTransactions);
        assertEquals(1L, summary.todayCreditTransactions);
        assertEquals(24L, summary.todayBonus);
        assertEquals(24L, summary.monthBonus);
    }

    @Test public void depositIncrementsDepositCounterAndContributesItsParsedBonus() {
        long receivedAt = utcDate(2026, Calendar.SEPTEMBER, 23, 10);
        SmsMessage deposit = SmsMessage.create("MVola",
                "Vous avez credite MARIETTE (0340677891) de 41 300 Ar le 23/09/26 "
                        + "a 10:00. Bonus:292 Ar. Solde : 87 115 Ar. Ref: 7576288436",
                receivedAt, true);

        SmsStatistics.TransactionSummary summary = SmsStatistics.summarize(
                Arrays.asList(deposit), receivedAt, 2026, Calendar.SEPTEMBER, UTC);

        assertEquals(1L, summary.todayTransactions);
        assertEquals(1L, summary.todayDepositTransactions);
        assertEquals(0L, summary.todayCreditTransactions);
        assertEquals(292L, summary.todayBonus);
        assertEquals(292L, summary.monthBonus);
    }

    private static SmsMessage deposit(String name, String number, long amount,
                                      String reference, long receivedAt) {
        return SmsMessage.create("Expéditeur quelconque",
                "Vous avez credite " + name + " (" + number + ") de " + amount
                        + " Ar. Ref: " + reference, receivedAt, true);
    }

    private static SmsMessage credit(String number, long amount, String reference,
                                     long receivedAt) {
        return SmsMessage.create("Expéditeur quelconque",
                "Achat de credit YAS reussi: " + amount + " Ar pour " + number
                        + ". Frais: 0 Ar. Ref: " + reference, receivedAt, true);
    }

    private static SmsMessage withdrawal(String number, long amount, String reference,
                                         int day, long receivedAt) {
        return SmsMessage.create("Expéditeur quelconque",
                amount + " Ar recu de CLIENT " + number + " le " + day
                        + "/09/26 a 10:00. Ref: " + reference, receivedAt, true);
    }

    private static long utcDate(int year, int month, int day, int hour) {
        Calendar calendar = Calendar.getInstance(UTC);
        calendar.clear();
        calendar.set(year, month, day, hour, 0);
        return calendar.getTimeInMillis();
    }

    private static void add(List<SmsMessage> messages, int count, int day) {
        for (int index = 0; index < count; index++) messages.add(sms(2026, 9, day, index));
    }

    private static SmsMessage sms(int year, int month, int day, int hour) {
        Calendar calendar = Calendar.getInstance(UTC);
        calendar.clear();
        calendar.set(year, month - 1, day, hour % 24, 0);
        String body = "1 000 Ar recu de Client 0343242318 le "
                + String.format(java.util.Locale.ROOT, "%02d/%02d/%02d a %02d:00. ",
                day, month, year % 100, hour % 24)
                + "Bonus:1 Ar. Solde: 10 000 Ar. Ref: 123456.";
        return SmsMessage.create("MVola", body, calendar.getTimeInMillis(), true);
    }

    private static SmsMessage mvola(String number, String time, long bonus, long receivedAt) {
        return SmsMessage.create("MVola", "1 000 Ar recu de Client Test " + number
                + " le 21/09/26 a " + time + ". Bonus:" + bonus
                + " Ar. Solde: 10 000 Ar. Ref: 123456.", receivedAt, true);
    }

    private static SmsMessage mvolaOnDay(String number, int day, String time) {
        return SmsMessage.create("MVola", "1 000 Ar recu de Client Test " + number
                + " le " + day + "/09/26 a " + time
                + ". Bonus:1 Ar. Solde: 10 000 Ar. Ref: 123456.", 1L, true);
    }
}
