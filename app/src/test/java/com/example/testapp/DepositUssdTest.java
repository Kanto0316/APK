package com.example.testapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class DepositUssdTest {
    @Test public void acceptsAndNormalizesMalagasyMobileNumbers() {
        assertEquals("0341234567", DepositUssd.normalizeRecipientNumber("0341234567"));
        assertEquals("0341234567", DepositUssd.normalizeRecipientNumber("+261341234567"));
        assertEquals("0341234567", DepositUssd.normalizeRecipientNumber("034 12 345 67"));
        assertEquals("0321234567", DepositUssd.normalizeRecipientNumber("032-12-345-67"));
        assertEquals("0381234567", DepositUssd.normalizeRecipientNumber("(038) 12 345 67"));
    }

    @Test public void rejectsUnsafeOrIncompleteNumbers() {
        assertNull(DepositUssd.normalizeRecipientNumber("034123"));
        assertNull(DepositUssd.normalizeRecipientNumber("034ABC4567"));
        assertNull(DepositUssd.normalizeRecipientNumber("03412*4567"));
        assertNull(DepositUssd.normalizeRecipientNumber("03412#4567"));
        assertNull(DepositUssd.normalizeRecipientNumber(""));
    }

    @Test public void validatesAndNormalizesAmounts() {
        assertNull(DepositUssd.normalizeAmount(""));
        assertNull(DepositUssd.normalizeAmount("0"));
        assertNull(DepositUssd.normalizeAmount("25 000"));
        assertNull(DepositUssd.normalizeAmount("25Ar"));
        assertNull(DepositUssd.normalizeAmount("25*000"));
        assertEquals("25000", DepositUssd.normalizeAmount("25000"));
        assertEquals("25", DepositUssd.normalizeAmount("00025"));
    }

    @Test public void buildsExactDepositUssdCode() {
        assertEquals("#111*1*2*0341234567*1*25000#",
                DepositUssd.buildUssdCode("0341234567", "25000"));
    }
}
