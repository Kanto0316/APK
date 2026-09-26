package com.netk.mvolatrack.history;

import static org.junit.Assert.assertEquals;

import com.netk.mvolatrack.database.SmsMessage;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class TodayHistorySummaryTest {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @Test public void totalsOnlySupportedTypesFromLocalToday() {
        long todayFallback = instant(2026, Calendar.SEPTEMBER, 25, 12, 0);
        List<SmsMessage> messages = Arrays.asList(
                sms("14 000 Ar recu de A 0341411050 le 25/09/26 a 08:19. Ref:1.", 1L),
                sms("7 500 Ar recu de B 0341411051 le 25/09/26 a 09:19. Ref:2.", 1L),
                sms("Vous avez crédité C (0341411052) de 200 Ar le 25/09/26 "
                        + "à 10:19. Ref:3.", 1L),
                sms("Achat de credit YAS reussi: 500 Ar pour 0341411053. Frais: 0 Ar. "
                        + "Solde MVola : 1 000 Ar. Ref: 4", todayFallback),
                sms("9 000 Ar recu de D 0341411054 le 24/09/26 a 23:59. Ref:5.", 1L));

        TodayHistorySummary summary = TodayHistorySummary.calculate(
                HistoryTransaction.fromMessages(messages), todayFallback, UTC);

        assertEquals(21_500L, summary.incoming);
        assertEquals(700L, summary.outgoing);
    }

    @Test public void returnsZeroWhenThereAreNoTransactionsToday() {
        TodayHistorySummary summary = TodayHistorySummary.calculate(
                HistoryTransaction.fromMessages(Arrays.asList(
                        sms("3 000 Ar recu de A 0341411050 le 24/09/26 a 08:19. Ref:1.", 1L))),
                instant(2026, Calendar.SEPTEMBER, 25, 12, 0), UTC);

        assertEquals(0L, summary.incoming);
        assertEquals(0L, summary.outgoing);
    }

    private static SmsMessage sms(String body, long receivedAt) {
        return SmsMessage.create("MVola", body, receivedAt, true);
    }

    private static long instant(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance(UTC);
        calendar.clear();
        calendar.set(year, month, day, hour, minute);
        return calendar.getTimeInMillis();
    }
}
