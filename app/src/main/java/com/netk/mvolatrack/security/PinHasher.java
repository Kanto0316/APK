package com.netk.mvolatrack.security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PinHasher {
    public static final int ITERATIONS = 120000;
    public static final int KEY_LENGTH = 256;
    private PinHasher() {}

    public static byte[] newSalt() {
        byte[] salt = new byte[32];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public static byte[] hash(char[] pin, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(pin, salt, iterations, KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("PIN hashing unavailable", exception);
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(pin, '\0');
        }
    }

    public static boolean verify(char[] pin, byte[] salt, byte[] expected, int iterations) {
        if (pin.length == 0) {
            java.util.Arrays.fill(pin, '\0');
            return false;
        }
        return MessageDigest.isEqual(hash(pin, salt, iterations), expected);
    }
}
