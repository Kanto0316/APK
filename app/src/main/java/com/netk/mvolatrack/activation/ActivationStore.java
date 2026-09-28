package com.netk.mvolatrack.activation;

import android.content.Context;
import android.content.SharedPreferences;

/** Stores the signed proof (never an activation boolean) and rollback guard state. */
public final class ActivationStore {
    private static final String PREFERENCES = "activation_license";
    private static final String KEY_PROOF = "mvact1_proof";
    private static final String KEY_LAST_VALID_TIME = "last_known_valid_time";
    static final long CLOCK_ROLLBACK_TOLERANCE_MS = 5 * 60 * 1000L;

    private ActivationStore() { }

    public static boolean hasValidActivation(Context context) {
        return status(context).result == ActivationVerifier.Result.VALID;
    }

    public static ActivationVerifier.Verification status(Context context) {
        String id = InstallationIdentity.getOrCreate(context);
        long now = System.currentTimeMillis();
        ActivationVerifier.Verification checked = ActivationVerifier.inspect(loadProof(context), id, () -> now);
        if (checked.result != ActivationVerifier.Result.VALID || checked.license == null
                || !checked.license.isTemporary()) return checked;

        SharedPreferences preferences = preferences(context);
        long last = preferences.getLong(KEY_LAST_VALID_TIME, 0);
        ActivationVerifier.Verification temporal = applyRollbackGuard(checked, now, last);
        if (temporal.result == ActivationVerifier.Result.CLOCK_ROLLBACK) return temporal;
        if (shouldAdvanceLastKnownValidTime(temporal, now, last)) {
            preferences.edit().putLong(KEY_LAST_VALID_TIME, now).apply();
        }
        return checked;
    }

    /** Applies the existing tolerance without mutating either the proof or the saved clock guard. */
    static ActivationVerifier.Verification applyRollbackGuard(
            ActivationVerifier.Verification checked, long now, long lastKnownValidTime) {
        // Small NTP/mobile-network corrections are harmless. A larger backwards jump is denied.
        if (checked.result == ActivationVerifier.Result.VALID && checked.license != null
                && checked.license.isTemporary() && lastKnownValidTime > 0
                && now + CLOCK_ROLLBACK_TOLERANCE_MS < lastKnownValidTime) {
            return new ActivationVerifier.Verification(
                    ActivationVerifier.Result.CLOCK_ROLLBACK, checked.license);
        }
        return checked;
    }

    static boolean shouldAdvanceLastKnownValidTime(ActivationVerifier.Verification checked,
            long now, long lastKnownValidTime) {
        return checked.result == ActivationVerifier.Result.VALID && checked.license != null
                && checked.license.isTemporary() && now > lastKnownValidTime;
    }

    public static String loadProof(Context context) {
        return preferences(context).getString(KEY_PROOF, null);
    }

    public static boolean saveVerifiedProof(Context context, String proof) {
        String id = InstallationIdentity.getOrCreate(context);
        long now = System.currentTimeMillis();
        ActivationVerifier.Verification checked = ActivationVerifier.inspect(proof, id, () -> now);
        if (checked.result != ActivationVerifier.Result.VALID) return false;
        SharedPreferences.Editor editor = preferences(context).edit().putString(KEY_PROOF, proof.trim());
        if (checked.license.isTemporary()) editor.putLong(KEY_LAST_VALID_TIME, now);
        else editor.remove(KEY_LAST_VALID_TIME);
        return editor.commit();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }
}
