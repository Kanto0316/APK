package com.netk.mvolatrack.activation;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ActivationProtocolTest {
    private static final String ID = "23456789ABCDEFGHJKMNPQRSTUVWXYZ2";

    @Test
    public void requestGenerationAndFormattingMatchAdminAlphabet() {
        String generated = ActivationRequest.generate(new SecureRandom());
        assertEquals(32, generated.length());
        for (char character : generated.toCharArray()) {
            assertFalse(ActivationRequest.ALPHABET.indexOf(character) < 0);
        }
        assertEquals("2345-6789-ABCD-EFGH-JKMN-PQRS-TUVW-XYZ2",
                ActivationRequest.format(ID));
        assertEquals(ID, ActivationRequest.parse(ActivationRequest.format(ID)));
    }

    @Test
    public void signedResponseIsVerifiedAndBoundToInstallation() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair pair = generator.generateKeyPair();
        String canonical = ActivationResponse.canonical(ID);
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(pair.getPrivate());
        signer.update(canonical.getBytes(StandardCharsets.UTF_8));
        String proof = "MVACT1." + url(canonical.getBytes(StandardCharsets.UTF_8))
                + "." + url(signer.sign());

        assertEquals(ActivationVerifier.Result.VALID,
                ActivationVerifier.verify(proof, ID, pair.getPublic()));
        assertEquals(ActivationVerifier.Result.WRONG_INSTALLATION,
                ActivationVerifier.verify(proof, "33456789ABCDEFGHJKMNPQRSTUVWXYZ2",
                        pair.getPublic()));
        assertEquals(ActivationVerifier.Result.INVALID,
                ActivationVerifier.verify(proof.substring(0, proof.length() - 1) + "A", ID,
                        pair.getPublic()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void paddedBase64UrlIsRejected() {
        ActivationResponse.parse("MVACT1.AA==.AA");
    }

    @Test
    public void v2UsesSignedExpiryBoundaryAndSupportsAllAdminTypes() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair pair = generator.generateKeyPair();
        long issuedAt = 1_700_000_000_000L;
        long expiresAt = 1_700_604_800_000L;

        for (ActivationResponse.LicenseType type : new ActivationResponse.LicenseType[]{
                ActivationResponse.LicenseType.WEEK, ActivationResponse.LicenseType.MONTH}) {
            String proof = signedV2(pair, type, issuedAt, expiresAt);
            assertEquals(ActivationVerifier.Result.VALID,
                    ActivationVerifier.inspect(proof, ID, pair.getPublic(), () -> expiresAt - 1).result);
            assertEquals(ActivationVerifier.Result.EXPIRED,
                    ActivationVerifier.inspect(proof, ID, pair.getPublic(), () -> expiresAt).result);
            assertEquals(ActivationVerifier.Result.EXPIRED,
                    ActivationVerifier.inspect(proof, ID, pair.getPublic(), () -> expiresAt + 1).result);
        }

        String permanent = signedV2(pair, ActivationResponse.LicenseType.PERMANENT, issuedAt, 0);
        assertEquals(ActivationVerifier.Result.VALID,
                ActivationVerifier.inspect(permanent, ID, pair.getPublic(), () -> Long.MAX_VALUE).result);
    }

    @Test
    public void v2CanonicalisationHasAdminFieldOrder() {
        assertEquals("MVOLACASH|2|WEEK|" + ID + "|1700000000000|1700604800000",
                ActivationResponse.canonicalV2(ActivationResponse.LicenseType.WEEK, ID,
                        1_700_000_000_000L, 1_700_604_800_000L));
    }

    private static String signedV2(KeyPair pair, ActivationResponse.LicenseType type,
            long issuedAt, long expiresAt) throws Exception {
        String canonical = ActivationResponse.canonicalV2(type, ID, issuedAt, expiresAt);
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(pair.getPrivate());
        signer.update(canonical.getBytes(StandardCharsets.UTF_8));
        return "MVACT1." + url(canonical.getBytes(StandardCharsets.UTF_8)) + "." + url(signer.sign());
    }

    private static String url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
