package com.netk.mvolatrack.verification;

/**
 * Immutable control record. It is deliberately separate from the original SMS transaction and
 * never changes a wallet balance.
 */
public final class TransactionBalanceVerification {
    public final Long ancienSolde;
    public final long montantTransaction;
    public final long frais;
    public final long bonusAttendu;
    public final Long soldeSansBonus;
    public final Long soldeAttendu;
    public final Long nouveauSoldeReel;
    public final Long bonusConstate;
    public final Long ecart;
    public final VerificationStatus statut;
    public final long dateVerification;
    public final String referenceTransaction;
    public final long dateTransaction;
    public final TransactionDirection typeTransaction;

    TransactionBalanceVerification(Long ancienSolde, long montantTransaction, long frais,
                                   long bonusAttendu, Long soldeSansBonus, Long soldeAttendu,
                                   Long nouveauSoldeReel, Long bonusConstate, Long ecart,
                                   VerificationStatus statut, long dateVerification,
                                   String referenceTransaction, long dateTransaction,
                                   TransactionDirection typeTransaction) {
        this.ancienSolde = ancienSolde;
        this.montantTransaction = montantTransaction;
        this.frais = frais;
        this.bonusAttendu = bonusAttendu;
        this.soldeSansBonus = soldeSansBonus;
        this.soldeAttendu = soldeAttendu;
        this.nouveauSoldeReel = nouveauSoldeReel;
        this.bonusConstate = bonusConstate;
        this.ecart = ecart;
        this.statut = statut;
        this.dateVerification = dateVerification;
        this.referenceTransaction = referenceTransaction;
        this.dateTransaction = dateTransaction;
        this.typeTransaction = typeTransaction;
    }
}
