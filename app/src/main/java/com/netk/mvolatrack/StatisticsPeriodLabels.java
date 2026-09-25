package com.netk.mvolatrack;

import java.text.DateFormatSymbols;
import java.util.Locale;

final class StatisticsPeriodLabels {
    private StatisticsPeriodLabels() {
    }

    static Labels getPeriodLabels(int selectedMonth, int selectedYear,
            int currentMonth, int currentYear) {
        if (selectedMonth == currentMonth && selectedYear == currentYear) {
            return new Labels("CE MOIS", "CETTE ANNÉE");
        }

        String month = new DateFormatSymbols(Locale.FRENCH).getMonths()[selectedMonth]
                .toUpperCase(Locale.FRENCH);
        return new Labels(month + " " + selectedYear, "ANNÉE " + selectedYear);
    }

    static final class Labels {
        final String month;
        final String year;

        Labels(String month, String year) {
            this.month = month;
            this.year = year;
        }
    }
}
