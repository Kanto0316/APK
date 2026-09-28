package com.netk.mvolatrack.activation;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

public final class ActivationVerifier {
    private static final String PUBLIC_KEY_X509_BASE64 =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEzzL4HKGuLEBxYJ0vNON3WnnVP9m5DRU8T1FMWnUye2Pmvp8N7T2Lma/UD/WEnYmokGwRIlCf/UrcCp5EfWr+PQ==";

    public enum Result { VALID, WRONG_INSTALLATION, INVALID }

    private ActivationVerifier() { }

    public static Result verify(String proof, String localInstallationId) {
        try {
            byte[] keyBytes = decodeStandardBase64(PUBLIC_KEY_X509_BASE64);
            PublicKey key = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(keyBytes));
            return verify(proof, localInstallationId, key);
        } catch (Exception error) {
            return Result.INVALID;
        }
    }

    static Result verify(String proof, String localInstallationId, PublicKey key) {
        try {
            ActivationResponse response = ActivationResponse.parse(proof);
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(key);
            verifier.update(response.getCanonicalPayload());
            if (!verifier.verify(response.getSignature())) return Result.INVALID;
            if (!response.getInstallationId().equals(ActivationRequest.parse(localInstallationId))) {
                return Result.WRONG_INSTALLATION;
            }
            return Result.VALID;
        } catch (Exception error) {
            return Result.INVALID;
        }
    }

    private static byte[] decodeStandardBase64(String value) {
        String url = value.replace('+', '-').replace('/', '_');
        while (url.endsWith("=")) url = url.substring(0, url.length() - 1);
        return Base64Url.decode(url);
    }
}
