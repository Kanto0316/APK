package com.netk.mvolatrack.verification;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TransactionBalanceVerifierTest {
    @Test public void outgoingExpectedBonusIsVerified() {
        TransactionBalanceVerification result = verify(100000L, 5000, 100, 48, 94948L, false);

        assertEquals(Long.valueOf(94900), result.soldeSansBonus);
        assertEquals(Long.valueOf(94948), result.soldeAttendu);
        assertEquals(Long.valueOf(48), result.bonusConstate);
        assertEquals(Long.valueOf(0), result.ecart);
        assertEquals(VerificationStatus.BONUS_VERIFIE, result.statut);
    }

    @Test public void missingBonusIsReported() {
        TransactionBalanceVerification result = verify(100000L, 5000, 100, 48, 94900L, false);

        assertEquals(Long.valueOf(0), result.bonusConstate);
        assertEquals(VerificationStatus.BONUS_NON_CREDITE, result.statut);
    }

    @Test public void partialBonusIsReported() {
        TransactionBalanceVerification result = verify(100000L, 5000, 100, 48, 94930L, false);

        assertEquals(Long.valueOf(30), result.bonusConstate);
        assertEquals(Long.valueOf(-18), result.ecart);
        assertEquals(VerificationStatus.BONUS_PARTIEL, result.statut);
    }

    @Test public void higherBonusIsReported() {
        TransactionBalanceVerification result = verify(100000L, 5000, 100, 48, 94960L, false);

        assertEquals(Long.valueOf(60), result.bonusConstate);
        assertEquals(Long.valueOf(12), result.ecart);
        assertEquals(VerificationStatus.BONUS_SUPERIEUR, result.statut);
    }

    @Test public void missingBalanceIsNotVerifiable() {
        TransactionBalanceVerification result = verify(null, 5000, 100, 48, 94948L, false);

        assertEquals(VerificationStatus.NON_VERIFIABLE, result.statut);
        assertNull(result.soldeAttendu);
        assertNull(result.bonusConstate);
    }

    @Test public void possibleIntermediateTransactionIsNotVerifiable() {
        TransactionBalanceVerification result = verify(100000L, 5000, 100, 48, 94948L, true);

        assertEquals(VerificationStatus.NON_VERIFIABLE, result.statut);
    }

    @Test public void incomingTransactionUsesAddition() {
        TransactionBalanceVerifier.Input input = input(100000L, 5000, 100, 48, 104948L, false);
        input.typeTransaction = TransactionDirection.ENTRANTE;

        TransactionBalanceVerification result =
                TransactionBalanceVerifier.verifyTransactionBalance(input);

        assertEquals(Long.valueOf(104900), result.soldeSansBonus);
        assertEquals(VerificationStatus.BONUS_VERIFIE, result.statut);
    }

    @Test public void transactionWithoutBonusChecksOnlyBalance() {
        TransactionBalanceVerification result = verify(100000L, 5000, 100, 0, 94900L, false);

        assertEquals(Long.valueOf(0), result.bonusConstate);
        assertEquals(VerificationStatus.BONUS_VERIFIE, result.statut);
    }

    private static TransactionBalanceVerification verify(Long oldBalance, long amount, long fee,
                                                          long bonus, Long newBalance,
                                                          boolean ambiguous) {
        return TransactionBalanceVerifier.verifyTransactionBalance(
                input(oldBalance, amount, fee, bonus, newBalance, ambiguous));
    }

    private static TransactionBalanceVerifier.Input input(Long oldBalance, long amount, long fee,
                                                          long bonus, Long newBalance,
                                                          boolean ambiguous) {
        TransactionBalanceVerifier.Input input = new TransactionBalanceVerifier.Input();
        input.ancienSolde = oldBalance;
        input.montantTransaction = amount;
        input.frais = fee;
        input.bonusAttendu = bonus;
        input.nouveauSoldeReel = newBalance;
        input.referenceTransaction = "12345";
        input.dateTransaction = 1L;
        input.typeTransaction = TransactionDirection.SORTANTE;
        input.correspondanceAmbigue = ambiguous;
        return input;
    }
}
