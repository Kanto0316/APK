package com.netk.mvolatrack.activation;

import java.io.ByteArrayOutputStream;

/** Strict RFC 4648 base64url decoder; MVACT1 deliberately forbids padding. */
final class Base64Url {
    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    private Base64Url() { }

    static byte[] decode(String value) {
        if (value == null || value.isEmpty() || value.indexOf('=') >= 0 || value.length() % 4 == 1) {
            throw new IllegalArgumentException("Invalid base64url");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(value.length() * 3 / 4);
        int buffer = 0;
        int bits = 0;
        for (int i = 0; i < value.length(); i++) {
            int digit = ALPHABET.indexOf(value.charAt(i));
            if (digit < 0) throw new IllegalArgumentException("Invalid base64url");
            buffer = (buffer << 6) | digit;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out.write((buffer >> bits) & 0xff);
            }
        }
        if (bits > 0 && (buffer & ((1 << bits) - 1)) != 0) {
            throw new IllegalArgumentException("Non-canonical base64url");
        }
        return out.toByteArray();
    }
}
