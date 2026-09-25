package com.netk.mvolatrack;

import static org.junit.Assert.assertEquals;

import com.netk.mvolatrack.database.SmsMessage;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;

public class BalanceTitleTest {
    @Test public void defaultsToZeroWithoutAParsedBalance() {
        assertEquals("0 Ar", BalanceTitle.from(Collections.emptyList()));
        assertEquals("0 Ar", BalanceTitle.from(Collections.singletonList(message(
                "1 500 Ar recu de Jean 0343242318 le 21/09/26 a 08:19. Ref: 1.", 10))));
    }

    @Test public void latestReceivedBalanceWinsEvenWhenItsBusinessDateIsOlder() {
        SmsMessage smsA = message(
                "3 000 Ar recu de LOVASOA 0343242318 le 22/09/26 a 14:10. "
                        + "Solde: 10 800 Ar. Ref: 1.", 1_000);
        SmsMessage smsB = message(
                "3 000 Ar recu de LOVASOA 0343242318 le 21/09/26 a 08:19. "
                        + "Solde: 8 800 Ar. Ref: 2.", 2_000);

        assertEquals(8_800L, BalanceTitle.latestBalance(Arrays.asList(smsA, smsB)));
        assertEquals("8 800 Ar", BalanceTitle.from(Arrays.asList(smsA, smsB)));
    }

    @Test public void formatsThousandsWithSpaces() {
        assertEquals("1 000 000 Ar", BalanceTitle.from(Collections.singletonList(message(
                "3 000 Ar recu de LOVASOA 0343242318 le 22/09/26 a 14:10. "
                        + "Solde: 1 000 000 Ar. Ref: 2.", 100))));
    }

    @Test public void highestRoomIdResolvesEqualReceptionDates() {
        SmsMessage higherId = message(
                "3 000 Ar recu de LOVASOA 0343242318 le 22/09/26 a 14:10. "
                        + "Solde: 200 000 Ar. Ref: 3.", 100);
        higherId.id = 2;
        SmsMessage lowerId = message(
                "3 000 Ar recu de LOVASOA 0343242318 le 22/09/26 a 14:10. "
                        + "Solde: 100 000 Ar. Ref: 2.", 100);
        lowerId.id = 1;

        assertEquals("200 000 Ar", BalanceTitle.from(
                Arrays.asList(lowerId, higherId)));
    }

    @Test public void newestBalanceWinsAcrossWithdrawalDepositAndCreditRegardlessOfAmount() {
        SmsMessage withdrawal = message(
                "3 000 Ar recu de LOVASOA 0343242318 le 24/09/26 a 10:00. "
                        + "Solde: 82 023 Ar. Ref: 1.", timestamp(24, 10, 0));
        SmsMessage deposit = message(
                "Vous avez credite LOVASOA (0343242318) de 3 000 Ar le 24/09/26 a 10:05. "
                        + "Bonus:24 Ar. Solde MVola : 87 115 Ar. Ref: 2",
                timestamp(24, 10, 5));
        SmsMessage credit = message(
                "Achat de credit YAS reussi: 1 000 Ar pour 0343242318. Frais: 0 Ar. "
                        + "Bonus:24 Ar. Solde MVola : 80 869 Ar. Ref: 3",
                timestamp(24, 10, 10));

        assertEquals(80_869L, BalanceTitle.latestBalance(
                Arrays.asList(deposit, credit, withdrawal)));
        assertEquals("80 869 Ar", BalanceTitle.from(
                Arrays.asList(deposit, credit, withdrawal)));

        SmsMessage newerDeposit = message(
                "Vous avez credite LOVASOA (0343242318) de 3 000 Ar le 24/09/26 a 10:15. "
                        + "Bonus:24 Ar. Solde MVola : 93 599 Ar. Ref: 4",
                timestamp(24, 10, 15));
        assertEquals("93 599 Ar", BalanceTitle.from(
                Arrays.asList(withdrawal, deposit, credit, newerDeposit)));

        SmsMessage newestWithdrawal = message(
                "3 000 Ar recu de LOVASOA 0343242318 le 24/09/26 a 10:20. "
                        + "Solde: 10 800 Ar. Ref: 5.", timestamp(24, 10, 20));
        assertEquals("10 800 Ar", BalanceTitle.from(
                Arrays.asList(newerDeposit, withdrawal, newestWithdrawal, deposit, credit)));
    }

    @Test public void skipsNewerTransactionWithoutBalance() {
        SmsMessage olderWithBalance = message(
                "Achat de credit YAS reussi: 1 000 Ar pour 0343242318. Frais: 0 Ar. "
                        + "Bonus:24 Ar. Solde MVola : 80 869 Ar. Ref: 3",
                timestamp(23, 10, 25));
        SmsMessage newerWithoutBalance = message(
                "Achat de credit YAS reussi: 1 000 Ar pour 0343242318. Ref: 4",
                timestamp(23, 10, 30));

        assertEquals("80 869 Ar", BalanceTitle.from(
                Arrays.asList(newerWithoutBalance, olderWithBalance)));
    }

    @Test public void keepsFullBalanceForHeaderAutoSizing() {
        long[] balances = {800L, 17_800L, 500_800L, 2_500_000L, 25_000_000L, 999_999_999L};
        String[] expected = {"800 Ar", "17 800 Ar", "500 800 Ar", "2 500 000 Ar",
                "25 000 000 Ar", "999 999 999 Ar"};

        for (int index = 0; index < balances.length; index++) {
            assertEquals(expected[index], BalanceTitle.from(Collections.singletonList(message(
                    "3 000 Ar recu de LOVASOA 0343242318 le 22/09/26 a 14:10. Solde: "
                            + expected[index] + ". Ref: " + index + ".", 100 + index))));
        }
    }

    private static long timestamp(int day, int hour, int minute) {
        return new GregorianCalendar(2026, 8, day, hour, minute).getTimeInMillis();
    }

    private static SmsMessage message(String body, long receivedDate) {
        return SmsMessage.create("MVola", body, receivedDate, true);
    }
}
