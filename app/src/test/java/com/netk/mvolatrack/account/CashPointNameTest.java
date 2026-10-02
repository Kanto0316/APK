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
}
