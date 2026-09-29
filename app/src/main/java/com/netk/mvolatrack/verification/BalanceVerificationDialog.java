package com.netk.mvolatrack.verification;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.netk.mvolatrack.R;

import java.text.NumberFormat;
import java.util.Locale;

/** Detailed mathematical evidence shown only after opening a history transaction. */
public final class BalanceVerificationDialog {
    private BalanceVerificationDialog() {}

    public static void show(Context context, TransactionBalanceVerification value) {
        View content = LayoutInflater.from(context)
                .inflate(R.layout.dialog_balance_verification, null, false);
        text(content, R.id.verificationReference, "Référence : "
                + safe(value.referenceTransaction));
        text(content, R.id.verificationCalculation, calculation(value));
        text(content, R.id.verificationProof, proof(value));
        TextView result = content.findViewById(R.id.verificationResult);
        result.setText(result(value));
        result.setTextColor(ContextCompat.getColor(context, color(value.statut)));
        new AlertDialog.Builder(context)
                .setView(content)
                .setNegativeButton("Fermer", null)
                .setPositiveButton("Revérifier", (dialog, which) -> show(context, value))
                .show();
    }

    public static String shortLabel(TransactionBalanceVerification value) {
        if (value == null) return "? Non vérifiable";
        switch (value.statut) {
            case BONUS_VERIFIE:
                return value.bonusAttendu == 0 ? "✓ Solde vérifié" : "✓ Bonus vérifié";
            case BONUS_NON_CREDITE: return "! Bonus non crédité";
            case BONUS_PARTIEL: return "! Bonus partiel";
            case NON_VERIFIABLE: return "? Non vérifiable";
            default: return "! Écart détecté";
        }
    }

    public static int color(VerificationStatus status) {
        if (status == VerificationStatus.BONUS_VERIFIE) return R.color.verification_success;
        if (status == VerificationStatus.BONUS_NON_CREDITE) return R.color.verification_error;
        if (status == VerificationStatus.NON_VERIFIABLE) return R.color.verification_unknown;
        return R.color.verification_warning;
    }

    private static String calculation(TransactionBalanceVerification value) {
        String direction = value.typeTransaction == TransactionDirection.ENTRANTE ? "+" : "−";
        return "Ancien solde       " + money(value.ancienSolde) + '\n'
                + "Montant          " + direction + money(value.montantTransaction) + '\n'
                + "Frais            −" + money(value.frais) + '\n'
                + "Bonus attendu    +" + money(value.bonusAttendu) + '\n'
                + "────────────────────────\n"
                + "Solde sans bonus " + money(value.soldeSansBonus) + '\n'
                + "Solde attendu    " + money(value.soldeAttendu) + '\n'
                + "Solde réel       " + money(value.nouveauSoldeReel) + '\n'
                + "Écart            " + signed(value.ecart) + '\n' + '\n'
                + "Bonus constaté   " + signed(value.bonusConstate);
    }

    private static String proof(TransactionBalanceVerification value) {
        if (value.statut == VerificationStatus.NON_VERIFIABLE) {
            return "Preuve indisponible : les deux relevés de solde ne peuvent pas être associés "
                    + "avec certitude. Une transaction intermédiaire est possible.";
        }
        String operator = value.typeTransaction == TransactionDirection.ENTRANTE ? " + " : " − ";
        return "Preuve :\n" + number(value.ancienSolde) + operator
                + number(value.montantTransaction) + " − " + number(value.frais) + " + "
                + number(value.bonusAttendu) + "\n= " + money(value.soldeAttendu);
    }

    private static String result(TransactionBalanceVerification value) {
        switch (value.statut) {
            case BONUS_VERIFIE:
                return value.bonusAttendu == 0 ? "✓ SOLDE VÉRIFIÉ"
                        : "✓ BONUS CORRECTEMENT APPLIQUÉ";
            case BONUS_NON_CREDITE:
                return "✕ BONUS NON CRÉDITÉ — manquant : " + money(value.bonusAttendu);
            case BONUS_PARTIEL:
                return "! BONUS PARTIEL — manquant : " + money(-value.ecart);
            case BONUS_SUPERIEUR:
                return "! BONUS SUPÉRIEUR — écart : " + signed(value.ecart);
            case ECART_INEXPLIQUE: return "! ÉCART DE SOLDE INEXPLIQUÉ";
            default: return "? NON VÉRIFIABLE";
        }
    }

    private static String money(Long value) {
        return value == null ? "—" : number(value) + " Ar";
    }

    private static String number(Long value) {
        return value == null ? "—" : NumberFormat.getIntegerInstance(Locale.FRENCH)
                .format(value).replace('\u202f', ' ').replace('\u00a0', ' ');
    }

    private static String signed(Long value) {
        if (value == null) return "—";
        return (value > 0 ? "+" : "") + money(value);
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value;
    }

    private static void text(View root, int id, String value) {
        ((TextView) root.findViewById(id)).setText(value);
    }
}
