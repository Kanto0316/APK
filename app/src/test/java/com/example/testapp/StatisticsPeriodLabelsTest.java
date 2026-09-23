package com.example.testapp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StatisticsPeriodLabelsTest {
    private static final int SEPTEMBER = 8;

    @Test
    public void currentMonthUsesGenericLabels() {
        assertLabels(SEPTEMBER, 2026, SEPTEMBER, 2026, "CE MOIS", "CETTE ANNÉE");
    }

    @Test
    public void anotherMonthUsesSelectedFrenchMonthAndYear() {
        assertLabels(7, 2026, SEPTEMBER, 2026, "AOÛT 2026", "ANNÉE 2026");
        assertLabels(11, 2025, SEPTEMBER, 2026, "DÉCEMBRE 2025", "ANNÉE 2025");
    }

    @Test
    public void yearTransitionsKeepTheSelectedYear() {
        assertLabels(11, 2025, 0, 2026, "DÉCEMBRE 2025", "ANNÉE 2025");
        assertLabels(0, 2027, 11, 2026, "JANVIER 2027", "ANNÉE 2027");
    }

    private void assertLabels(int selectedMonth, int selectedYear,
            int currentMonth, int currentYear, String expectedMonth, String expectedYear) {
        StatisticsPeriodLabels.Labels labels = StatisticsPeriodLabels.getPeriodLabels(
                selectedMonth, selectedYear, currentMonth, currentYear);
        assertEquals(expectedMonth, labels.month);
        assertEquals(expectedYear, labels.year);
    }
}
