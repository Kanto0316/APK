package com.example.testapp;

import static org.junit.Assert.assertEquals;

import com.example.testapp.database.SmsMessage;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ClientMessageGrouperTest {
    @Test public void group_deduplicatesSendersAndSortsClientsAndMessagesByRecency() {
        SmsMessage oldMvola = SmsMessage.create("MVola", "ancien", 100L, true);
        SmsMessage other = SmsMessage.create("0341411058", "numéro", 200L, true);
        SmsMessage newMvola = SmsMessage.create("MVola", "récent", 300L, true);

        List<ClientMessageGrouper.ClientGroup> groups = ClientMessageGrouper.group(
                Arrays.asList(oldMvola, other, newMvola));

        assertEquals(2, groups.size());
        assertEquals("MVola", groups.get(0).sender);
        assertEquals(2, groups.get(0).messages.size());
        assertEquals("récent", groups.get(0).messages.get(0).messageBody);
        assertEquals("0341411058", groups.get(1).sender);
    }

    @Test public void group_emptySourceHasNoFictitiousClient() {
        assertEquals(Collections.emptyList(), ClientMessageGrouper.group(Collections.emptyList()));
    }
}
