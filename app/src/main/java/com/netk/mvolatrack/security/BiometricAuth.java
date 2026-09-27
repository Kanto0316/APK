package com.netk.mvolatrack.security;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;
import java.util.concurrent.Executor;

final class BiometricAuth {
    private static final int AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG;
    static boolean isAvailable(FragmentActivity activity) {
        try {
            return BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS)
                    == BiometricManager.BIOMETRIC_SUCCESS;
        } catch (RuntimeException exception) {
            return false;
        }
    }
    static void authenticate(FragmentActivity activity, Runnable success) {
        Executor executor = androidx.core.content.ContextCompat.getMainExecutor(activity);
        BiometricPrompt prompt = new BiometricPrompt(activity, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        success.run();
                    }
                });
        prompt.authenticate(new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Déverrouiller MVolaCash")
                .setSubtitle("Confirmez votre identité")
                .setNegativeButtonText("Utiliser le code PIN")
                .setAllowedAuthenticators(AUTHENTICATORS).build());
    }
    private BiometricAuth() {}
}
