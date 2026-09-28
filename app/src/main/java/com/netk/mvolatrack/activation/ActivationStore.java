package com.netk.mvolatrack.activation;

import android.content.Context;
import android.content.SharedPreferences;

/** Stores the signed proof, never an activation boolean. */
public final class ActivationStore {
    private static final String PREFERENCES = "activation_license";
    private static final String KEY_PROOF = "mvact1_proof";

    private ActivationStore() { }

    public static boolean hasValidActivation(Context context) {
        String id = InstallationIdentity.getOrCreate(context);
        return ActivationVerifier.verify(loadProof(context), id) == ActivationVerifier.Result.VALID;
    }

    public static String loadProof(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getString(KEY_PROOF, null);
    }

    public static boolean saveVerifiedProof(Context context, String proof) {
        String id = InstallationIdentity.getOrCreate(context);
        if (ActivationVerifier.verify(proof, id) != ActivationVerifier.Result.VALID) return false;
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
                .putString(KEY_PROOF, proof.trim()).commit();
    }
}
