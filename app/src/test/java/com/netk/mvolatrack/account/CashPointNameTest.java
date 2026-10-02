package com.netk.mvolatrack.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CashPointNameTest {
    @Test public void emptyAndWhitespaceOnlyNamesAreRejected() {
        assertFalse(CashPointName.isValid(null));
        assertFalse(CashPointName.isValid("  \n "));
    }

    @Test public void nameIsTrimmedAndWhitespaceCollapsed() {
        assertEquals("Cash point Ravaka", CashPointName.normalize("  Cash   point\nRavaka  "));
        assertTrue(CashPointName.isValid(" Ravaka "));
    }

    @Test public void normalizedNameIsLimitedToEighteenCharacters() {
        assertTrue(CashPointName.isValid("123456789012345678"));
        assertFalse(CashPointName.isValid("1234567890123456789"));
        assertTrue(CashPointName.isValid("  123456789   12345678  "));
    }
}
