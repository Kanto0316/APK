package com.netk.mvolatrack.activation;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.SecureRandom;

/** Creates and retains an anonymous, app-private installation identity. */
public final class InstallationIdentity {
    private static final String PREFERENCES = "activation_identity";
    private static final String KEY_INSTALLATION_ID = "installation_id";

    private InstallationIdentity() { }

    public static synchronized String getOrCreate(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        String existing = preferences.getString(KEY_INSTALLATION_ID, null);
        try {
            if (existing != null) return ActivationRequest.parse(existing);
        } catch (IllegalArgumentException ignored) {
            // Corrupt local state is replaced with a fresh identity, requiring a new activation.
        }
        String generated = ActivationRequest.generate(new SecureRandom());
        preferences.edit().putString(KEY_INSTALLATION_ID, generated).commit();
        return generated;
    }
}
