package com.netk.mvolatrack.invoice;

import android.content.Context;
import android.content.SharedPreferences;

/** Durable, process-safe allocator whose value is committed only after a successful write. */
public final class InvoiceNumberStore {
    private static final String PREFS = "invoice_numbers";
    private static final String NEXT = "next_number";
    private static final Object LOCK = new Object();
    private final SharedPreferences preferences;

    public InvoiceNumberStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public long peek() {
        synchronized (LOCK) { return Math.max(1L, preferences.getLong(NEXT, 1L)); }
    }

    /** Call while no other invoice export can run. Failed commits do not consume the number. */
    public boolean consume(long number) {
        synchronized (LOCK) {
            long next = Math.max(1L, preferences.getLong(NEXT, 1L));
            if (number != next) return false;
            return preferences.edit().putLong(NEXT, next + 1L).commit();
        }
    }

    public static String format(long number) {
        return String.format(java.util.Locale.US, "%06d", number);
    }
}
