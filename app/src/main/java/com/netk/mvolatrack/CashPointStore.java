package com.netk.mvolatrack;

import android.content.Context;
import android.content.SharedPreferences;

/** One locally configured cash point name for this installation. */
public final class CashPointStore {
    public static final int MAX_NAME_LENGTH = 24;
    private static final String PREFERENCES = "cash_point_account";
    private static final String KEY_NAME = "cash_point_name";

    private CashPointStore() { }

    public static String getName(Context context) {
        String name = preferences(context).getString(KEY_NAME, "");
        return name == null ? "" : name;
    }

    public static boolean isConfigured(Context context) {
        return !getName(context).trim().isEmpty();
    }

    public static boolean saveName(Context context, String value) {
        String name = normalize(value);
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) return false;
        return preferences(context).edit().putString(KEY_NAME, name).commit();
    }

    public static String invoiceBrand(Context context) {
        String name = getName(context);
        return name.isEmpty() ? "MVolaCash" : "Cash point " + name;
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }
}
