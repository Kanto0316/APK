package com.netk.mvolatrack.notification;

final class NotificationMessagePresentation {
    private static final String DAILY_SUMMARY_METADATA_PREFIX = "daily_summary:";
    private static final String LEGACY_DAILY_SUMMARY_SUCCESS =
            "Le SMS a été accepté par Android.";
    private static final String DAILY_SUMMARY_SUCCESS =
            "Le SMS a été envoyé avec succès.";

    private NotificationMessagePresentation() { }

    static boolean isDailySummary(String metadata) {
        return metadata != null && metadata.startsWith(DAILY_SUMMARY_METADATA_PREFIX);
    }

    static String dailySummaryId(String metadata) {
        return metadata.substring(DAILY_SUMMARY_METADATA_PREFIX.length());
    }

    static String displayedMessage(String metadata, String message) {
        if (isDailySummary(metadata) && message != null
                && message.contains(LEGACY_DAILY_SUMMARY_SUCCESS)) {
            return message.replace(LEGACY_DAILY_SUMMARY_SUCCESS, DAILY_SUMMARY_SUCCESS);
        }
        return message;
    }
}
