package com.netk.mvolatrack.activation;

import java.security.SecureRandom;
import java.util.Locale;

/** Public request-code format shared with MVolaCash Admin. */
public final class ActivationRequest {
    public static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    public static final int ID_LENGTH = 32;

    private ActivationRequest() { }

    public static String generate(SecureRandom random) {
        StringBuilder result = new StringBuilder(ID_LENGTH);
        for (int i = 0; i < ID_LENGTH; i++) {
            result.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return result.toString();
    }

    public static String parse(String requestCode) {
        if (requestCode == null) throw new IllegalArgumentException("Missing request code");
        String canonical = requestCode.trim().replace("-", "").toUpperCase(Locale.US);
        if (canonical.length() != ID_LENGTH) throw new IllegalArgumentException("Invalid length");
        for (int i = 0; i < canonical.length(); i++) {
            if (ALPHABET.indexOf(canonical.charAt(i)) < 0) {
                throw new IllegalArgumentException("Invalid character");
            }
        }
        return canonical;
    }

    public static String format(String installationId) {
        String canonical = parse(installationId);
        StringBuilder formatted = new StringBuilder(39);
        for (int i = 0; i < canonical.length(); i++) {
            if (i > 0 && i % 4 == 0) formatted.append('-');
            formatted.append(canonical.charAt(i));
        }
        return formatted.toString();
    }
}
