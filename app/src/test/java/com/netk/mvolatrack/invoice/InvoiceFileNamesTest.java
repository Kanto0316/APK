package com.netk.mvolatrack.invoice;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InvoiceFileNamesTest {
    @Test public void keepsSafeReferenceCharacters() {
        assertEquals("ABC-12_3.4", InvoiceFileNames.sanitizeReference("ABC-12_3.4"));
    }

    @Test public void replacesUnsafeCharactersAndNeverReturnsEmptyName() {
        assertEquals("AB_12", InvoiceFileNames.sanitizeReference("  AB / 12? "));
        assertEquals("transaction", InvoiceFileNames.sanitizeReference("..."));
        assertEquals("transaction", InvoiceFileNames.sanitizeReference(null));
    }
}
