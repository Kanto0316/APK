package com.netk.mvolatrack.sms;

import android.content.Context;
import android.content.SharedPreferences;

/** Persists receiver milestones so they remain inspectable after the process is recreated. */
public final class SmsReceptionDiagnostics {
    private static final String PREFERENCES = "sms_receiver_diagnostics";
    private static final String LAST_RECEIVED_AT = "last_received_at";
    private static final String LAST_ROOM_INSERT_AT = "last_room_insert_at";

    private SmsReceptionDiagnostics() { }

    public static void recordReceiverInvocation(Context context, long timestamp) {
        preferences(context).edit().putLong(LAST_RECEIVED_AT, timestamp).commit();
    }

    public static void recordRoomInsertion(Context context, long timestamp) {
        preferences(context).edit().putLong(LAST_ROOM_INSERT_AT, timestamp).commit();
    }

    public static long getLastReceiverInvocation(Context context) {
        return preferences(context).getLong(LAST_RECEIVED_AT, 0L);
    }

    public static long getLastRoomInsertion(Context context) {
        return preferences(context).getLong(LAST_ROOM_INSERT_AT, 0L);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }
}
