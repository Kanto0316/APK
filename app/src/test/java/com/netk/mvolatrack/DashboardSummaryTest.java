package com.netk.mvolatrack;

import static org.junit.Assert.assertEquals;

import com.netk.mvolatrack.database.SmsMessage;
import com.netk.mvolatrack.history.HistoryTransaction;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

public class DashboardSummaryTest {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @Test public void derivesDashboardValuesOnlyFromPersistedTransactions() {
        SmsMessage older = SmsMessage.create("MVola",
                "1 000 Ar recu de A 0341411050 le 29/09/26 a 08:19. "
                        + "Bonus:20 Ar. Solde:10 000 Ar. Ref:1.", 1L, true);
        SmsMessage today = SmsMessage.create("MVola",
                "2 000 Ar recu de A 0341411050 le 30/09/26 a 09:20. "
                        + "Bonus:40 Ar. Solde:12 040 Ar. Ref:2.", 2L, true);
        Calendar now = Calendar.getInstance(UTC);
        now.clear();
        now.set(2026, Calendar.SEPTEMBER, 30, 12, 0);

        DashboardSummary summary = DashboardSummary.from(
                HistoryTransaction.fromMessages(Arrays.asList(older, today)),
                now.getTimeInMillis(), UTC);

        assertEquals(2, summary.transactionCount);
        assertEquals(1, summary.todayTransactionCount);
        assertEquals(1, summary.clientCount);
        assertEquals(0, summary.newClientCount);
        assertEquals(1, summary.activeClientCount);
        assertEquals("0341411050", summary.mostActiveClient);
        assertEquals(2, summary.mostActiveClientTransactions);
        assertEquals(3000L, summary.withdrawalTotal);
        assertEquals(60L, summary.bonusTotal);
        assertEquals(12040L, summary.balance);
        assertEquals(2, summary.latestTransactions.size());
    }
}
