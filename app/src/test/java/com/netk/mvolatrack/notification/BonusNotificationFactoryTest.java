package com.netk.mvolatrack.notification;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.netk.mvolatrack.verification.VerificationStatus;
import com.netk.mvolatrack.database.NotificationBonus;
import com.netk.mvolatrack.database.NotificationType;
import org.junit.Test;

public class BonusNotificationFactoryTest {
    @Test public void generalNotificationStoresCategoryAndDisplayContent() {
        NotificationBonus notification = NotificationBonus.general(123L, "system-update",
                NotificationType.SYSTEME, "Mise à jour", "Une mise à jour est disponible.", null);

        assertEquals("SYSTEME", notification.type);
        assertEquals("Mise à jour", notification.title);
        assertEquals("Une mise à jour est disponible.", notification.message);
        assertEquals(123L, notification.date);
        assertFalse(notification.isRead);
    }
    @Test public void onlyBonusAnomaliesCreateNotifications() {
        assertFalse(BonusNotificationFactory.shouldNotify(VerificationStatus.BONUS_VERIFIE));
        assertFalse(BonusNotificationFactory.shouldNotify(VerificationStatus.ECART_INEXPLIQUE));
        assertTrue(BonusNotificationFactory.shouldNotify(VerificationStatus.NON_VERIFIABLE));
        assertTrue(BonusNotificationFactory.shouldNotify(VerificationStatus.BONUS_SUPERIEUR));
        assertTrue(BonusNotificationFactory.shouldNotify(VerificationStatus.BONUS_NON_CREDITE));
        assertTrue(BonusNotificationFactory.shouldNotify(VerificationStatus.BONUS_PARTIEL));
    }
}
