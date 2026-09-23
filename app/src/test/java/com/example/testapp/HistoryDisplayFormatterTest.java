package com.example.testapp;

import static org.junit.Assert.assertEquals;

import com.example.testapp.database.SmsMessage;
import com.example.testapp.history.HistoryTransaction;

import org.junit.Test;

import java.util.Arrays;

public class HistoryDisplayFormatterTest {
    @Test public void formatsRequiredWithdrawalCardValues() {
        SmsMessage sms = SmsMessage.create("MVola",
                "3 000 Ar recu de Client 0341411050 le 21/09/26 a 08:19. "
                        + "Bonus:58 Ar. Ref:7193727366.", 1L, true);
        HistoryTransaction item = HistoryTransaction.fromMessages(Arrays.asList(sms)).get(0);

        assertEquals("034 14 110 50", HistoryDisplayFormatter.number(item.clientNumber));
        assertEquals("+3 000 Ar", HistoryDisplayFormatter.amount(item));
        assertEquals("Réf 7193727366", HistoryDisplayFormatter.reference(item.reference));
        assertEquals("58 Ar", HistoryDisplayFormatter.bonus(item.bonus));
    }

    @Test public void formatsMissingAndExplicitZeroValuesDifferently() {
        assertEquals("Réf -", HistoryDisplayFormatter.reference(null));
        assertEquals("-", HistoryDisplayFormatter.bonus(null));
        assertEquals("0 Ar", HistoryDisplayFormatter.bonus(0L));
    }
}
