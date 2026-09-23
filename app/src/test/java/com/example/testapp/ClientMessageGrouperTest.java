package com.example.testapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.testapp.database.SmsMessage;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ClientMessageGrouperTest {
    @Test public void group_usesOnlyExtractedClientAndSortsMessagesByRecency() {
        SmsMessage old = transaction("0343242318", "ancien", 100L);
        SmsMessage ignored = SmsMessage.create("MVola", "format inconnu", 300L, true);
        SmsMessage recent = transaction("0343242318", "récent", 200L);

        List<ClientMessageGrouper.ClientGroup> groups = ClientMessageGrouper.group(
                Arrays.asList(old, ignored, recent));

        assertEquals(1, groups.size());
        assertEquals("0343242318", groups.get(0).sender);
        assertEquals(2, groups.get(0).messages.size());
        assertEquals(recent.messageBody, groups.get(0).messages.get(0).messageBody);
    }

    @Test public void group_unparsedMvolaDoesNotCreateClient() {
        SmsMessage raw = SmsMessage.create("MVola", "message non reconnu", 100L, true);
        assertEquals(Collections.emptyList(), ClientMessageGrouper.group(
                Collections.singletonList(raw)));
        assertNull(ClientMessageGrouper.clientKey(raw));
        assertEquals("message non reconnu", raw.messageBody);
        assertEquals("MVola", raw.sender);
    }

    @Test public void filter_matchesExtractedNumberWithSpacesAndPartialQueries() {
        List<ClientMessageGrouper.ClientGroup> groups = ClientMessageGrouper.group(Arrays.asList(
                transaction("0344153878", "un", 100L),
                transaction("0384900247", "deux", 200L)));

        for (String query : Arrays.asList("0344153878", "034 41", "53878")) {
            assertEquals(query, 1, ClientMessageGrouper.filter(groups, query,
                    ClientMessageGrouper.Filter.ALL).size());
        }
        assertEquals(0, ClientMessageGrouper.filter(groups, "MVola",
                ClientMessageGrouper.Filter.ALL).size());
    }

    @Test public void filtersUseParsedTransactionCount() {
        List<ClientMessageGrouper.ClientGroup> groups = ClientMessageGrouper.group(Arrays.asList(
                transaction("0343242318", "premier", 100L),
                transaction("0343242318", "deuxième", 200L),
                transaction("0384900247", "unique", 300L),
                SmsMessage.create("MVola", "ignoré", 400L, true)));

        assertEquals(1, ClientMessageGrouper.filter(groups, "",
                ClientMessageGrouper.Filter.NEW).size());
        assertEquals("0384900247", ClientMessageGrouper.filter(groups, "",
                ClientMessageGrouper.Filter.NEW).get(0).sender);
        assertEquals(1, ClientMessageGrouper.filter(groups, "",
                ClientMessageGrouper.Filter.EXISTING).size());
    }

    private static SmsMessage transaction(String number, String label, long receivedAt) {
        return SmsMessage.create("MVola", "1 000 Ar recu de " + label + " " + number
                + " le 21/09/26 a 08:19. Bonus:1 Ar. Solde: 10 000 Ar. Ref: 123456.",
                receivedAt, true);
    }
}
