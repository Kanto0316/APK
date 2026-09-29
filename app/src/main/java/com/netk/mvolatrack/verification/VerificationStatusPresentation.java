package com.netk.mvolatrack.verification;

import com.netk.mvolatrack.R;

/** Single status-to-UI mapping shared by every transaction table. */
public final class VerificationStatusPresentation {
    public final int label;
    public final int icon;
    public final int color;

    private VerificationStatusPresentation(int label, int icon, int color) {
        this.label = label;
        this.icon = icon;
        this.color = color;
    }

    public static VerificationStatusPresentation from(TransactionBalanceVerification value) {
        VerificationStatus status = value == null ? VerificationStatus.NON_VERIFIABLE : value.statut;
        if (status == null) status = VerificationStatus.NON_VERIFIABLE;
        switch (status) {
            case BONUS_VERIFIE:
                return new VerificationStatusPresentation(R.string.sms_status_verified,
                        R.drawable.ic_status_verified_20, R.color.verification_success);
            case BONUS_NON_CREDITE:
                return new VerificationStatusPresentation(R.string.sms_status_bonus_not_credited,
                        R.drawable.ic_status_error_20, R.color.verification_error);
            case BONUS_PARTIEL:
                return new VerificationStatusPresentation(R.string.sms_status_partial_bonus,
                        R.drawable.ic_status_warning_20, R.color.verification_warning);
            case BONUS_SUPERIEUR:
                return new VerificationStatusPresentation(R.string.sms_status_higher_bonus,
                        R.drawable.ic_status_warning_20, R.color.verification_warning);
            case ECART_INEXPLIQUE:
                return new VerificationStatusPresentation(R.string.sms_status_balance_difference,
                        R.drawable.ic_status_warning_20, R.color.verification_warning);
            default:
                return new VerificationStatusPresentation(R.string.sms_status_not_verifiable,
                        R.drawable.ic_status_unknown_20, R.color.verification_unknown);
        }
    }
}
