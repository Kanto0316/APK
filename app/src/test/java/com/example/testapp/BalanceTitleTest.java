package com.example.testapp;

import static org.junit.Assert.assertEquals;

import com.example.testapp.database.SmsMessage;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class BalanceTitleTest {
    @Test public void defaultsToZeroWithoutAParsedBalance() {
        assertEquals("Solde : 0 Ar >", BalanceTitle.from(Collections.emptyList()));
        assertEquals("Solde : 0 Ar >", BalanceTitle.from(Collections.singletonList(message(
                "1 500 Ar recu de Jean 0343242318 le 21/09/26 a 08:19. Ref: 1.", 10))));
    }

    @Test public void usesTransactionDateRatherThanListOrderOrReceptionDate() {
        SmsMessage newerTransactionReceivedFirst = message(
                "3 000 Ar recu de LOVASOA 0343242318 le 22/09/26 a 14:10. "
                        + "Solde: 147 479 Ar. Ref: 2.", 100);
        SmsMessage olderTransactionReceivedLast = message(
                "3 000 Ar recu de LOVASOA 0343242318 le 21/09/26 a 08:19. "
                        + "Solde: 82 023 Ar. Ref: 1.", 200);

        assertEquals("Solde : 147 479 Ar >", BalanceTitle.from(
                Arrays.asList(olderTransactionReceivedLast, newerTransactionReceivedFirst)));
    }

    @Test public void formatsThousandsWithSpaces() {
        assertEquals("Solde : 1 000 000 Ar >", BalanceTitle.from(Collections.singletonList(message(
                "3 000 Ar recu de LOVASOA 0343242318 le 22/09/26 a 14:10. "
                        + "Solde: 1 000 000 Ar. Ref: 2.", 100))));
    }

    @Test public void usesReceptionDateToResolveEqualTransactionTimes() {
        SmsMessage receivedLater = message(
                "3 000 Ar recu de LOVASOA 0343242318 le 22/09/26 a 14:10. "
                        + "Solde: 200 000 Ar. Ref: 3.", 300);
        SmsMessage receivedEarlier = message(
                "3 000 Ar recu de LOVASOA 0343242318 le 22/09/26 a 14:10. "
                        + "Solde: 100 000 Ar. Ref: 2.", 100);

        assertEquals("Solde : 200 000 Ar >", BalanceTitle.from(
                Arrays.asList(receivedEarlier, receivedLater)));
    }

    private static SmsMessage message(String body, long receivedDate) {
        return SmsMessage.create("MVola", body, receivedDate, true);
    }
}
