package com.example.testapp;

import static org.junit.Assert.assertEquals;

import com.example.testapp.database.SmsMessage;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;

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

    @Test public void newestBalanceWinsAcrossWithdrawalDepositAndCreditRegardlessOfAmount() {
        SmsMessage withdrawal = message(
                "3 000 Ar recu de LOVASOA 0343242318 le 21/09/26 a 08:19. "
                        + "Solde: 82 023 Ar. Ref: 1.", timestamp(21, 8, 20));
        SmsMessage deposit = message(
                "Vous avez credite LOVASOA (0343242318) de 3 000 Ar le 22/09/26 a 14:10. "
                        + "Bonus:24 Ar. Solde MVola : 93 599 Ar. Ref: 2",
                timestamp(22, 14, 11));
        SmsMessage credit = message(
                "Achat de credit YAS reussi: 1 000 Ar pour 0343242318. Frais: 0 Ar. "
                        + "Bonus:24 Ar. Solde MVola : 80 869 Ar. Ref: 3",
                timestamp(23, 10, 0));

        assertEquals(80_869L, BalanceTitle.latestBalance(
                Arrays.asList(deposit, credit, withdrawal)));
        assertEquals("Solde : 80 869 Ar >", BalanceTitle.from(
                Arrays.asList(deposit, credit, withdrawal)));
    }

    @Test public void skipsNewerTransactionWithoutBalance() {
        SmsMessage olderWithBalance = message(
                "Achat de credit YAS reussi: 1 000 Ar pour 0343242318. Frais: 0 Ar. "
                        + "Bonus:24 Ar. Solde MVola : 80 869 Ar. Ref: 3",
                timestamp(23, 10, 25));
        SmsMessage newerWithoutBalance = message(
                "Achat de credit YAS reussi: 1 000 Ar pour 0343242318. Ref: 4",
                timestamp(23, 10, 30));

        assertEquals("Solde : 80 869 Ar >", BalanceTitle.from(
                Arrays.asList(newerWithoutBalance, olderWithBalance)));
    }

    private static long timestamp(int day, int hour, int minute) {
        return new GregorianCalendar(2026, 8, day, hour, minute).getTimeInMillis();
    }

    private static SmsMessage message(String body, long receivedDate) {
        return SmsMessage.create("MVola", body, receivedDate, true);
    }
}
