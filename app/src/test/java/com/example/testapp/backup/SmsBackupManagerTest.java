package com.example.testapp.backup;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SmsBackupManagerTest {
    @Test
    public void acceptsAllSmsBackupNames() {
        assertTrue(SmsBackupManager.isBackupFileName("sms-history.smsbackup"));
        assertTrue(SmsBackupManager.isBackupFileName("sms-history (1).smsbackup"));
        assertTrue(SmsBackupManager.isBackupFileName("archive.SMSBACKUP"));
    }

    @Test
    public void rejectsUnrelatedAndTemporaryFiles() {
        assertFalse(SmsBackupManager.isBackupFileName("sms-history.smsbackup.tmp"));
        assertFalse(SmsBackupManager.isBackupFileName("sms-history.json"));
        assertFalse(SmsBackupManager.isBackupFileName(null));
    }
}
