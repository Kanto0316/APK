package com.netk.mvolatrack.account;

import android.content.Context;
import android.content.SharedPreferences;

/** Installation-wide cash-point identity. This is deliberately unrelated to transaction users. */
public final class AccountStore {
    private static final String PREFERENCES = "cash_point_account";
    private static final String KEY_NAME = "cash_point_name";

    private AccountStore() {}

    public static String getName(Context context) {
        return preferences(context).getString(KEY_NAME, "");
    }

    public static boolean hasName(Context context) {
        return CashPointName.isValid(getName(context));
    }

    public static boolean saveName(Context context, String value) {
        String normalized = CashPointName.normalize(value);
        if (!CashPointName.isValid(normalized)) return false;
        return preferences(context).edit().putString(KEY_NAME, normalized).commit();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFERENCES,
                Context.MODE_PRIVATE);
    }
}
