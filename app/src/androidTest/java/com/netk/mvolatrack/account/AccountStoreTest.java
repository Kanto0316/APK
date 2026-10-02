package com.netk.mvolatrack.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Test;

public class AccountStoreTest {
    private final Context context = ApplicationProvider.getApplicationContext();

    @After public void clearPreferences() {
        context.getSharedPreferences("cash_point_account", Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test public void nameAndCanonicalNumberPersistTogether() {
        assertTrue(AccountStore.save(context, "  Point Ravaka  ", "+261 34 12 345 67"));
        assertEquals("Point Ravaka", AccountStore.getName(context));
        assertEquals("0341234567", AccountStore.getNumber(context));
    }

    @Test public void legacyAccountWithoutNumberRemainsConfigured() {
        context.getSharedPreferences("cash_point_account", Context.MODE_PRIVATE).edit()
                .putString("cash_point_name", "Ancien surnom dépassant 18 caractères")
                .remove("cash_point_number")
                .commit();

        assertTrue(AccountStore.hasName(context));
        assertFalse(AccountStore.hasNumber(context));
        assertEquals("Ancien surnom dépassant 18 caractères", AccountStore.getName(context));
    }
}
