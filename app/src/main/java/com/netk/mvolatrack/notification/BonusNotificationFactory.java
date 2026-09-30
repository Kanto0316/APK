package com.netk.mvolatrack.notification;

import com.netk.mvolatrack.database.NotificationBonus;
import com.netk.mvolatrack.history.HistoryTransaction;
import com.netk.mvolatrack.verification.TransactionBalanceVerification;
import com.netk.mvolatrack.verification.VerificationStatus;

/** Converts verifier output into inbox records without recomputing any bonus. */
public final class BonusNotificationFactory {
    private BonusNotificationFactory() {}

    public static boolean shouldNotify(VerificationStatus status) {
        return status == VerificationStatus.NON_VERIFIABLE
                || status == VerificationStatus.BONUS_SUPERIEUR
                || status == VerificationStatus.BONUS_NON_CREDITE
                || status == VerificationStatus.BONUS_PARTIEL;
    }

    public static NotificationBonus from(HistoryTransaction transaction, long createdAt) {
        if (transaction == null || transaction.verification == null
                || !shouldNotify(transaction.verification.statut)
                || transaction.sourceUniqueKey == null) return null;
        TransactionBalanceVerification value = transaction.verification;
        return new NotificationBonus(createdAt, transaction.sourceUniqueKey,
                transaction.reference, transaction.clientNumber, value.statut.name(),
                value.bonusAttendu, value.bonusConstate, explanation(value.statut),
                transaction.timestamp, false);
    }

    public static String explanation(VerificationStatus status) {
        if (status == VerificationStatus.BONUS_NON_CREDITE)
            return "Le bonus attendu n’a pas été crédité sur le nouveau solde.";
        if (status == VerificationStatus.BONUS_PARTIEL)
            return "Le bonus reçu est inférieur au bonus attendu.";
        if (status == VerificationStatus.BONUS_SUPERIEUR)
            return "Le bonus reçu est supérieur au bonus attendu.";
        return "Les informations disponibles ne permettent pas de vérifier ce bonus.";
    }
}
