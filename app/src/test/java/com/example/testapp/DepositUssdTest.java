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
        assertEquals("25000", DepositUssd.normalizeAmount("25 000"));
        assertNull(DepositUssd.normalizeAmount("25Ar"));
        assertNull(DepositUssd.normalizeAmount("25*000"));
        assertEquals("25000", DepositUssd.normalizeAmount("25000"));
        assertEquals("25", DepositUssd.normalizeAmount("00025"));
    }

    @Test public void formatsRecipientInputProgressivelyAndFromPaste() {
        assertEquals("0", DepositUssd.formatRecipientInput("0"));
        assertEquals("034", DepositUssd.formatRecipientInput("034"));
        assertEquals("034 1", DepositUssd.formatRecipientInput("0341"));
        assertEquals("034 14 110 58", DepositUssd.formatRecipientInput("0341411058"));
        assertEquals("034 14 110 58", DepositUssd.formatRecipientInput("034 14 110 58"));
        assertEquals("034 14 110 58", DepositUssd.formatRecipientInput("+261341411058"));
    }

    @Test public void formatsAmountsWithSpaceThousandsSeparators() {
        assertEquals("", DepositUssd.formatAmountInput(""));
        assertEquals("1", DepositUssd.formatAmountInput("1"));
        assertEquals("10", DepositUssd.formatAmountInput("10"));
        assertEquals("100", DepositUssd.formatAmountInput("100"));
        assertEquals("999", DepositUssd.formatAmountInput("999"));
        assertEquals("1 000", DepositUssd.formatAmountInput("1000"));
        assertEquals("2 000", DepositUssd.formatAmountInput("2000"));
        assertEquals("10 000", DepositUssd.formatAmountInput("10000"));
        assertEquals("20 000", DepositUssd.formatAmountInput("20 000"));
        assertEquals("100 000", DepositUssd.formatAmountInput("100000"));
        assertEquals("1 000 000", DepositUssd.formatAmountInput("1000000"));
    }

    @Test public void formatsAmountPasteAndProgressiveDeletionSafely() {
        assertEquals("20 000", DepositUssd.formatAmountInput("20000"));
        assertEquals("20 000", DepositUssd.formatAmountInput("20 000"));
        assertEquals("2 000", DepositUssd.formatAmountInput("2000"));
        assertEquals("200", DepositUssd.formatAmountInput("200"));
        assertEquals("20", DepositUssd.formatAmountInput("20"));
        assertEquals("2", DepositUssd.formatAmountInput("2"));
        assertEquals("", DepositUssd.formatAmountInput(""));
    }

    @Test public void buildsExactDepositUssdCode() {
        assertEquals("#111*1*2*0341234567*1*25000#",
                DepositUssd.buildUssdCode("0341234567", "25000"));
        assertEquals("#111*1*2*0341411058*1*20000#",
                DepositUssd.buildUssdCode("034 14 110 58", "20 000"));
    }

    @Test public void calculatesWithdrawalFeesAtEveryTariffBoundary() {
        assertNull(DepositUssd.calculateWithdrawalFee(1000));
        assertEquals(Long.valueOf(150), DepositUssd.calculateWithdrawalFee(1001));
        assertEquals(Long.valueOf(150), DepositUssd.calculateWithdrawalFee(5000));
        assertEquals(Long.valueOf(275), DepositUssd.calculateWithdrawalFee(5001));
        assertEquals(Long.valueOf(275), DepositUssd.calculateWithdrawalFee(10000));
        assertEquals(Long.valueOf(550), DepositUssd.calculateWithdrawalFee(10001));
        assertEquals(Long.valueOf(550), DepositUssd.calculateWithdrawalFee(20000));
        assertEquals(Long.valueOf(650), DepositUssd.calculateWithdrawalFee(20001));
        assertEquals(Long.valueOf(650), DepositUssd.calculateWithdrawalFee(25000));
        assertEquals(Long.valueOf(1300), DepositUssd.calculateWithdrawalFee(25001));
        assertEquals(Long.valueOf(1300), DepositUssd.calculateWithdrawalFee(50000));
        assertEquals(Long.valueOf(1900), DepositUssd.calculateWithdrawalFee(50001));
        assertEquals(Long.valueOf(1900), DepositUssd.calculateWithdrawalFee(100000));
        assertEquals(Long.valueOf(3400), DepositUssd.calculateWithdrawalFee(100001));
        assertEquals(Long.valueOf(3400), DepositUssd.calculateWithdrawalFee(200000));
        assertNull(DepositUssd.calculateWithdrawalFee(200001));
    }

    @Test public void finalAmountIsDerivedOnceFromOriginalAmount() {
        long originalAmount = 20000;
        assertEquals(20550, DepositUssd.calculateFinalAmount(originalAmount, true));
        assertEquals(20550, DepositUssd.calculateFinalAmount(originalAmount, true));
        assertEquals(20000, DepositUssd.calculateFinalAmount(originalAmount, false));
        assertEquals("#111*1*2*0341411058*1*20550#",
                DepositUssd.buildUssdCode("0341411058", String.valueOf(
                        DepositUssd.calculateFinalAmount(originalAmount, true))));
    }
}
