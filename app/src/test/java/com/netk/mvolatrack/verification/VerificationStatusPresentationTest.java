package com.netk.mvolatrack.verification;

import static org.junit.Assert.assertEquals;

import com.netk.mvolatrack.R;

import org.junit.Test;

public class VerificationStatusPresentationTest {
    @Test public void mapsEveryStatusAndAbsentResult() {
        assertPresentation(VerificationStatus.BONUS_VERIFIE, R.string.sms_status_verified,
                R.drawable.ic_status_verified_20, R.color.verification_success);
        assertPresentation(VerificationStatus.BONUS_NON_CREDITE,
                R.string.sms_status_bonus_not_credited,
                R.drawable.ic_status_error_20, R.color.verification_error);
        assertPresentation(VerificationStatus.BONUS_PARTIEL, R.string.sms_status_partial_bonus,
                R.drawable.ic_status_warning_20, R.color.verification_warning);
        assertPresentation(VerificationStatus.BONUS_SUPERIEUR, R.string.sms_status_higher_bonus,
                R.drawable.ic_status_warning_20, R.color.verification_warning);
        assertPresentation(VerificationStatus.ECART_INEXPLIQUE,
                R.string.sms_status_balance_difference,
                R.drawable.ic_status_warning_20, R.color.verification_warning);
        assertPresentation(VerificationStatus.NON_VERIFIABLE,
                R.string.sms_status_not_verifiable,
                R.drawable.ic_status_unknown_20, R.color.verification_unknown);

        VerificationStatusPresentation absent = VerificationStatusPresentation.from(null);
        assertEquals(R.string.sms_status_not_verifiable, absent.label);
        assertEquals(R.drawable.ic_status_unknown_20, absent.icon);
        assertEquals(R.color.verification_unknown, absent.color);
    }

    private static void assertPresentation(VerificationStatus status, int label, int icon,
                                           int color) {
        TransactionBalanceVerification value = new TransactionBalanceVerification(null, 0, 0,
                0, null, null, null, null, null, status, 0, null, 0,
                TransactionDirection.SORTANTE);
        VerificationStatusPresentation presentation = VerificationStatusPresentation.from(value);
        assertEquals(label, presentation.label);
        assertEquals(icon, presentation.icon);
        assertEquals(color, presentation.color);
    }
}
