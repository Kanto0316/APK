package com.example.testapp.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.testapp.database.SmsMessage;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class HistoryTransactionTest {
    @Test public void mapsRequiredWithdrawalFromPersistedSms() {
        SmsMessage sms = SmsMessage.create("MVola",
                "3 000 Ar recu de Client 0341411050 le 21/09/26 a 08:19. "
                        + "Bonus:58 Ar. Solde:82 023 Ar. Ref:7193727366.",
                1L, true);

        HistoryTransaction transaction = HistoryTransaction.fromMessages(
                Arrays.asList(sms)).get(0);

        assertEquals("Retrait", transaction.type);
        assertEquals("0341411050", transaction.clientNumber);
        assertEquals(3000L, transaction.amount);
        assertEquals(Long.valueOf(58L), transaction.bonus);
        assertEquals("7193727366", transaction.reference);
    }

    @Test public void ignoresInvalidSmsAndSortsByParsedTransactionDateDescending() {
        SmsMessage older = SmsMessage.create("MVola",
                "3 000 Ar recu de A 0341411050 le 20/09/26 a 08:19. Ref:1.", 999L, true);
        SmsMessage newer = SmsMessage.create("MVola",
                "11 000 Ar recu de B 0381453472 le 21/09/26 a 08:31. Ref:2.", 1L, true);
        SmsMessage invalid = SmsMessage.create("MVola", "Message non transactionnel", 2L, true);

        List<HistoryTransaction> result = HistoryTransaction.fromMessages(
                Arrays.asList(older, invalid, newer));

        assertEquals(2, result.size());
        assertEquals("2", result.get(0).reference);
        assertEquals("1", result.get(1).reference);
        assertNull(result.get(0).bonus);
    }
}
