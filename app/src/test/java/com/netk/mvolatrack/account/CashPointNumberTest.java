package com.netk.mvolatrack.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CashPointNumberTest {
    @Test public void acceptsNationalAndInternationalFormsWithSpaces() {
        assertEquals("0341234567", CashPointNumber.normalize("034 12 345 67"));
        assertEquals("0341234567", CashPointNumber.normalize("+261 34 12 345 67"));
        assertEquals("034 12 345 67", CashPointNumber.format("+261341234567"));
    }

    @Test public void rejectsMissingOrMalformedNumbers() {
        assertNull(CashPointNumber.normalize(null));
        assertNull(CashPointNumber.normalize(""));
        assertNull(CashPointNumber.normalize("034-12-345-67"));
        assertNull(CashPointNumber.normalize("261341234567"));
        assertFalse(CashPointNumber.isValid("034123456"));
        assertTrue(CashPointNumber.isValid("0201234567"));
    }
}
