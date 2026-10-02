package com.netk.mvolatrack.invoice;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.print.PrintJobInfo;
import org.junit.Test;

public final class InvoicePrintTrackerTest {
    @Test public void onlyCompletedPrintJobsAreSuccessful() {
        assertTrue(InvoicePrintTracker.isCompleted(PrintJobInfo.STATE_COMPLETED));
        assertFalse(InvoicePrintTracker.isCompleted(PrintJobInfo.STATE_CANCELED));
        assertFalse(InvoicePrintTracker.isCompleted(PrintJobInfo.STATE_FAILED));
        assertFalse(InvoicePrintTracker.isCompleted(PrintJobInfo.STATE_CREATED));
        assertFalse(InvoicePrintTracker.isCompleted(PrintJobInfo.STATE_STARTED));
        assertFalse(InvoicePrintTracker.isCompleted(PrintJobInfo.STATE_BLOCKED));
        assertFalse(InvoicePrintTracker.isCompleted(PrintJobInfo.STATE_QUEUED));
    }
}
