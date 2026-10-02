package com.netk.mvolatrack.invoice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public final class InvoiceResultNotificationHelperTest {
    @Test public void resultKeyProducesStableDistinctNotificationId() {
        int download = InvoiceResultNotificationHelper.notificationId("invoice-download-content://1");
        assertEquals(download,
                InvoiceResultNotificationHelper.notificationId("invoice-download-content://1"));
        assertNotEquals(download,
                InvoiceResultNotificationHelper.notificationId("invoice-print-attempt-1"));
    }
}
