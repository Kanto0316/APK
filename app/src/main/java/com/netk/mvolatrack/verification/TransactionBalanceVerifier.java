package com.netk.mvolatrack.verification;

/** Pure, reusable integer-only arithmetic for checking a reported wallet balance. */
public final class TransactionBalanceVerifier {
    private TransactionBalanceVerifier() {}

    public static TransactionBalanceVerification verifyTransactionBalance(Input input) {
        long checkedAt = System.currentTimeMillis();
        if (input == null) return unavailable(null, checkedAt);
        if (input.ancienSolde == null || input.nouveauSoldeReel == null
                || input.montantTransaction < 0 || input.frais < 0 || input.bonusAttendu < 0
                || input.typeTransaction == null || input.correspondanceAmbigue) {
            return unavailable(input, checkedAt);
        }

        long signedAmount = input.typeTransaction == TransactionDirection.ENTRANTE
                ? input.montantTransaction : -input.montantTransaction;
        long withoutBonus;
        long expected;
        try {
            withoutBonus = Math.subtractExact(Math.addExact(input.ancienSolde, signedAmount),
                    input.frais);
            expected = Math.addExact(withoutBonus, input.bonusAttendu);
        } catch (ArithmeticException overflow) {
            return unavailable(input, checkedAt);
        }
        long observedBonus;
        long difference;
        try {
            observedBonus = Math.subtractExact(input.nouveauSoldeReel, withoutBonus);
            difference = Math.subtractExact(input.nouveauSoldeReel, expected);
        } catch (ArithmeticException overflow) {
            return unavailable(input, checkedAt);
        }
        VerificationStatus status;
        if (observedBonus == input.bonusAttendu && difference == 0) {
            status = VerificationStatus.BONUS_VERIFIE;
        } else if (input.bonusAttendu > 0 && observedBonus == 0) {
            status = VerificationStatus.BONUS_NON_CREDITE;
        } else if (observedBonus > 0 && observedBonus < input.bonusAttendu) {
            status = VerificationStatus.BONUS_PARTIEL;
        } else if (observedBonus > input.bonusAttendu) {
            status = VerificationStatus.BONUS_SUPERIEUR;
        } else {
            status = VerificationStatus.ECART_INEXPLIQUE;
        }
        return new TransactionBalanceVerification(input.ancienSolde, input.montantTransaction,
                input.frais, input.bonusAttendu, withoutBonus, expected,
                input.nouveauSoldeReel, observedBonus, difference, status, checkedAt,
                input.referenceTransaction, input.dateTransaction, input.typeTransaction);
    }

    private static TransactionBalanceVerification unavailable(Input input, long checkedAt) {
        return new TransactionBalanceVerification(input == null ? null : input.ancienSolde,
                input == null ? 0 : input.montantTransaction,
                input == null ? 0 : input.frais, input == null ? 0 : input.bonusAttendu,
                null, null, input == null ? null : input.nouveauSoldeReel, null, null,
                VerificationStatus.NON_VERIFIABLE, checkedAt,
                input == null ? null : input.referenceTransaction,
                input == null ? 0 : input.dateTransaction,
                input == null ? null : input.typeTransaction);
    }

    public static final class Input {
        public Long ancienSolde;
        public long montantTransaction;
        public long frais;
        public long bonusAttendu;
        public Long nouveauSoldeReel;
        public String referenceTransaction;
        public long dateTransaction;
        public TransactionDirection typeTransaction;
        /** True when another SMS/transaction may be included between the two balance readings. */
        public boolean correspondanceAmbigue;
    }
}
