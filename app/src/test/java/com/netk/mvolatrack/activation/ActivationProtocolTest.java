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

    private static String url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
