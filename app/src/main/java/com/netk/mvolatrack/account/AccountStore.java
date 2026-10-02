package com.netk.mvolatrack.account;

import android.content.Context;
import android.content.SharedPreferences;

/** Installation-wide cash-point identity. This is deliberately unrelated to transaction users. */
public final class AccountStore {
    private static final String PREFERENCES = "cash_point_account";
    private static final String KEY_NAME = "cash_point_name";
    private static final String KEY_NUMBER = "cash_point_number";

    private AccountStore() {}

    public static String getName(Context context) {
        return preferences(context).getString(KEY_NAME, "");
    }

    public static boolean hasName(Context context) {
        // A legacy name may exceed today's editing limit. It must not lock the user out.
        return !CashPointName.normalize(getName(context)).isEmpty();
    }

    public static String getNumber(Context context) {
        return preferences(context).getString(KEY_NUMBER, "");
    }

    public static boolean hasNumber(Context context) {
        return CashPointNumber.isValid(getNumber(context));
    }

    public static boolean save(Context context, String name, String number) {
        String normalizedName = CashPointName.normalize(name);
        String normalizedNumber = CashPointNumber.normalize(number);
        if (!CashPointName.isValid(normalizedName) || normalizedNumber == null) return false;
        return preferences(context).edit()
                .putString(KEY_NAME, normalizedName)
                .putString(KEY_NUMBER, normalizedNumber)
                .commit();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFERENCES,
                Context.MODE_PRIVATE);
    }
}
