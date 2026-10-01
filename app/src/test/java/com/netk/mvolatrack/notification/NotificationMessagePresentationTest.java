package com.netk.mvolatrack.notification;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class NotificationMessagePresentationTest {
    private static final String LEGACY_MESSAGE = "Le SMS a été accepté par Android.";

    @Test public void replacesLegacyMessageForDailySummary() {
        assertEquals("Le SMS a été envoyé avec succès.",
                NotificationMessagePresentation.displayedMessage(
                        "daily_summary:42", LEGACY_MESSAGE));
    }

    @Test public void replacesLegacyTextWhenDailySummaryMessageContainsIt() {
        assertEquals("Info : Le SMS a été envoyé avec succès.",
                NotificationMessagePresentation.displayedMessage(
                        "daily_summary:42", "Info : " + LEGACY_MESSAGE));
    }

    @Test public void preservesSameMessageForOtherNotificationCategories() {
        assertEquals(LEGACY_MESSAGE,
                NotificationMessagePresentation.displayedMessage(
                        "licence:42", LEGACY_MESSAGE));
        assertEquals(LEGACY_MESSAGE,
                NotificationMessagePresentation.displayedMessage(null, LEGACY_MESSAGE));
    }
}
