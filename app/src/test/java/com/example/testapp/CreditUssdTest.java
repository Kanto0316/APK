package com.example.testapp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CreditUssdTest {
    @Test public void resolvesShortcutsOnlyWhenSubmitted() {
        assertEquals("1", DepositUssd.formatAmountInput("1"));
        assertEquals("2", DepositUssd.formatAmountInput("2"));
        assertEquals(500, CreditUssd.resolveAmount("1"));
        assertEquals(1000, CreditUssd.resolveAmount("2"));
    }

    @Test public void acceptsOnlyTheCreditRange() {
        assertEquals(100, CreditUssd.resolveAmount("100"));
        assertEquals(25000, CreditUssd.resolveAmount("25 000"));
        assertEquals(500000, CreditUssd.resolveAmount("500000"));
        assertEquals(-1, CreditUssd.resolveAmount("99"));
        assertEquals(-1, CreditUssd.resolveAmount("500001"));
    }

    @Test public void formatsAndBoundsCreditEditingSafely() {
        assertEquals("25 000", CreditUssd.formatBoundedAmountInput("25000", ""));
        assertEquals("500 000", CreditUssd.formatBoundedAmountInput("500000", ""));
        assertEquals("500 000", CreditUssd.formatBoundedAmountInput("500001", "500 000"));
        assertEquals("", CreditUssd.formatBoundedAmountInput("", "500 000"));
    }

    @Test public void buildsExactCreditUssdCodes() {
        assertEquals("#111*1*4*2*1*0341411058*500#",
                CreditUssd.buildUssdCode("0341411058", CreditUssd.resolveAmount("1")));
        assertEquals("#111*1*4*2*1*0341411058*1000#",
                CreditUssd.buildUssdCode("034 14 110 58", CreditUssd.resolveAmount("2")));
        assertEquals("#111*1*4*2*1*0341411058*25000#",
                CreditUssd.buildUssdCode("0341411058", CreditUssd.resolveAmount("25 000")));
    }
}
