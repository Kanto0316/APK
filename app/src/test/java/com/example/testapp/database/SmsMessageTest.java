package com.example.testapp.database;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class SmsMessageTest {
    @Test public void sameSmsHasStableFingerprint() {
        assertEquals(SmsMessage.fingerprint("TELCO", 123L, "Bonjour"),
                SmsMessage.fingerprint("TELCO", 123L, "Bonjour"));
    }

    @Test public void fingerprintUsesSenderDateAndCompleteBody() {
        String original = SmsMessage.fingerprint("TELCO", 123L, "Bonjour");
        assertNotEquals(original, SmsMessage.fingerprint("BANK", 123L, "Bonjour"));
        assertNotEquals(original, SmsMessage.fingerprint("TELCO", 124L, "Bonjour"));
        assertNotEquals(original, SmsMessage.fingerprint("TELCO", 123L, "Bonsoir"));
    }
}
