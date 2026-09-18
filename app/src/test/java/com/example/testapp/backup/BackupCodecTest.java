package com.example.testapp.backup;

import com.example.testapp.database.SmsMessage;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class BackupCodecTest {
    private static final char[] PASSWORD = "mot-de-passe-solide".toCharArray();

    @Test
    public void encryptedRoundTripPreservesMessages() throws Exception {
        SmsMessage source = SmsMessage.create("Banque", "Paiement de 42,00 EUR", 123456789L, true);
        byte[] backup = BackupCodec.encode(Arrays.asList(source), PASSWORD);

        assertFalse(new String(backup, StandardCharsets.ISO_8859_1).contains(source.messageBody));
        List<SmsMessage> restored = BackupCodec.decode(backup, PASSWORD);
        assertEquals(1, restored.size());
        assertEquals(source.sender, restored.get(0).sender);
        assertEquals(source.messageBody, restored.get(0).messageBody);
        assertEquals(source.receivedDate, restored.get(0).receivedDate);
        assertEquals(source.readStatus, restored.get(0).readStatus);
        assertEquals(source.uniqueKey, restored.get(0).uniqueKey);
    }

    @Test
    public void wrongPasswordIsRejected() throws Exception {
        byte[] backup = BackupCodec.encode(Arrays.asList(
                SmsMessage.create("Service", "Code 1234", 99L, false)), PASSWORD);
        try {
            BackupCodec.decode(backup, "autre-mot-de-passe".toCharArray());
            fail("A wrong password must not decrypt the backup");
        } catch (BackupCodec.BackupException expected) {
            assertEquals("Mot de passe incorrect ou sauvegarde corrompue", expected.getMessage());
        }
    }

    @Test
    public void truncatedBackupIsRejected() throws Exception {
        byte[] backup = BackupCodec.encode(Arrays.asList(
                SmsMessage.create("Service", "Message", 99L, false)), PASSWORD);
        try {
            BackupCodec.decode(Arrays.copyOf(backup, backup.length / 2), PASSWORD);
            fail("A truncated backup must not be accepted");
        } catch (BackupCodec.BackupException expected) {
            // Expected corruption path.
        }
    }
}
