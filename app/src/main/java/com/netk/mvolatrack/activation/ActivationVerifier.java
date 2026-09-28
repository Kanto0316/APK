package com.netk.mvolatrack.activation;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

public final class ActivationVerifier {
    private static final String PUBLIC_KEY_X509_BASE64 =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEzzL4HKGuLEBxYJ0vNON3WnnVP9m5DRU8T1FMWnUye2Pmvp8N7T2Lma/UD/WEnYmokGwRIlCf/UrcCp5EfWr+PQ==";

    public interface TimeSource { long currentTimeMillis(); }
    public enum Result { VALID, EXPIRED, WRONG_INSTALLATION, INVALID }

    public static final class Verification {
        public final Result result;
        public final ActivationResponse license;
        Verification(Result result, ActivationResponse license) {
            this.result = result;
            this.license = license;
        }
    }

    private ActivationVerifier() { }

    public static Result verify(String proof, String localInstallationId) {
        return inspect(proof, localInstallationId, System::currentTimeMillis).result;
    }

    public static Verification inspect(String proof, String localInstallationId, TimeSource clock) {
        try {
            byte[] keyBytes = decodeStandardBase64(PUBLIC_KEY_X509_BASE64);
            PublicKey key = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(keyBytes));
            return inspect(proof, localInstallationId, key, clock);
        } catch (Exception error) {
            return new Verification(Result.INVALID, null);
        }
    }

    static Result verify(String proof, String localInstallationId, PublicKey key) {
        return inspect(proof, localInstallationId, key, System::currentTimeMillis).result;
    }

    static Verification inspect(String proof, String localInstallationId, PublicKey key,
            TimeSource clock) {
        try {
            ActivationResponse response = ActivationResponse.parse(proof);
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(key);
            verifier.update(response.getCanonicalPayload());
            if (!verifier.verify(response.getSignature())) return new Verification(Result.INVALID, null);
            if (!response.getInstallationId().equals(ActivationRequest.parse(localInstallationId))) {
                return new Verification(Result.WRONG_INSTALLATION, response);
            }
            if (response.isTemporary() && clock.currentTimeMillis() >= response.getExpiresAt()) {
                return new Verification(Result.EXPIRED, response);
            }
            return new Verification(Result.VALID, response);
        } catch (Exception error) {
            return new Verification(Result.INVALID, null);
        }
    }

    private static byte[] decodeStandardBase64(String value) {
        String url = value.replace('+', '-').replace('/', '_');
        while (url.endsWith("=")) url = url.substring(0, url.length() - 1);
        return Base64Url.decode(url);
    }
}
