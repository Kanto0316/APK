package com.netk.mvolatrack.integrity;

import org.junit.Test;

import java.security.MessageDigest;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppIntegrityCheckerTest {
    @Test public void configuredCertificateIsAccepted() throws Exception {
        byte[] certificate = new byte[] { 1, 2, 3, 4 };
        String hash = hex(MessageDigest.getInstance("SHA-256").digest(certificate));
        assertTrue(AppIntegrityChecker.certificateBytesAreTrusted(new byte[][] { certificate },
                Collections.singleton(hash), true));
    }

    @Test public void unknownCertificateIsRejectedEvenWhenOtherStateCouldBeValid() {
        byte[] unknown = new byte[] { 9, 9, 9 };
        assertFalse(AppIntegrityChecker.certificateBytesAreTrusted(new byte[][] { unknown },
                Collections.singleton("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"),
                true));
    }

    @Test public void missingOrMalformedConfigurationFailsClosed() {
        assertTrue(AppIntegrityChecker.parseTrustedFingerprints("").isEmpty());
        assertTrue(AppIntegrityChecker.parseTrustedFingerprints("not-a-certificate").isEmpty());
        assertFalse(AppIntegrityChecker.parseTrustedFingerprints(
                "AA:AA:AA:AA:AA:AA:AA:AA:AA:AA:AA:AA:AA:AA:AA:AA:"
                + "AA:AA:AA:AA:AA:AA:AA:AA:AA:AA:AA:AA:AA:AA:AA:AA").isEmpty());
    }

    private static String hex(byte[] bytes) {
        StringBuilder value = new StringBuilder();
        for (byte item : bytes) value.append(String.format("%02X", item & 0xff));
        return value.toString();
    }
}
