package com.netk.mvolatrack.notification;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.netk.mvolatrack.verification.VerificationStatus;
import org.junit.Test;

public class BonusNotificationFactoryTest {
    @Test public void onlyBonusAnomaliesCreateNotifications() {
        assertFalse(BonusNotificationFactory.shouldNotify(VerificationStatus.BONUS_VERIFIE));
        assertFalse(BonusNotificationFactory.shouldNotify(VerificationStatus.ECART_INEXPLIQUE));
        assertTrue(BonusNotificationFactory.shouldNotify(VerificationStatus.NON_VERIFIABLE));
        assertTrue(BonusNotificationFactory.shouldNotify(VerificationStatus.BONUS_SUPERIEUR));
        assertTrue(BonusNotificationFactory.shouldNotify(VerificationStatus.BONUS_NON_CREDITE));
        assertTrue(BonusNotificationFactory.shouldNotify(VerificationStatus.BONUS_PARTIEL));
    }
}
