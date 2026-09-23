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

    @Test public void filter_matchesNumbersWithSpacesAndPartialQueries() {
        List<ClientMessageGrouper.ClientGroup> groups = ClientMessageGrouper.group(Arrays.asList(
                SmsMessage.create("034 41 538 78", "message", 100L, true),
                SmsMessage.create("MVola", "service", 200L, true)));

        assertEquals("034 41 538 78", ClientMessageGrouper.filter(groups, "0344153878",
                ClientMessageGrouper.Filter.ALL).get(0).sender);
        assertEquals("034 41 538 78", ClientMessageGrouper.filter(groups, "034 41",
                ClientMessageGrouper.Filter.ALL).get(0).sender);
        assertEquals("034 41 538 78", ClientMessageGrouper.filter(groups, "53878",
                ClientMessageGrouper.Filter.ALL).get(0).sender);
        assertEquals("MVola", ClientMessageGrouper.filter(groups, "mvola",
                ClientMessageGrouper.Filter.ALL).get(0).sender);
    }

    @Test public void filter_preservesLeadingZeroAndMatchesAllLocalNumberForms() {
        List<ClientMessageGrouper.ClientGroup> groups = ClientMessageGrouper.group(
                Collections.singletonList(
                        SmsMessage.create("038 49 002 47", "message", 100L, true)));

        for (String query : Arrays.asList(
                "03849", "3849", "0384900247", "038 49", "49 002", "00247")) {
            assertEquals(query, 1, ClientMessageGrouper.filter(
                    groups, query, ClientMessageGrouper.Filter.ALL).size());
        }
    }

    @Test public void filter_matchesInternationalMalagasyNumberWithLocalAndCountryCodeQueries() {
        List<ClientMessageGrouper.ClientGroup> groups = ClientMessageGrouper.group(
                Collections.singletonList(
                        SmsMessage.create("+261384900247", "message", 100L, true)));

        for (String query : Arrays.asList("03849", "3849", "2613849", "+2613849")) {
            assertEquals(query, 1, ClientMessageGrouper.filter(
                    groups, query, ClientMessageGrouper.Filter.ALL).size());
        }
    }

    @Test public void filter_matchesTextSendersIgnoringCaseAndPresentationSeparators() {
        List<ClientMessageGrouper.ClientGroup> groups = ClientMessageGrouper.group(
                Collections.singletonList(SmsMessage.create("MVola", "service", 100L, true)));

        for (String query : Arrays.asList("MVola", "mvola", "MVO", "vola")) {
            assertEquals(query, 1, ClientMessageGrouper.filter(
                    groups, query, ClientMessageGrouper.Filter.ALL).size());
        }
    }

    @Test public void filter_numberSearchWorksWithEveryClientFilter() {
        List<ClientMessageGrouper.ClientGroup> newClient = ClientMessageGrouper.group(
                Collections.singletonList(
                        SmsMessage.create("038 49 002 47", "unique", 100L, true)));
        assertEquals(1, ClientMessageGrouper.filter(
                newClient, "03849", ClientMessageGrouper.Filter.ALL).size());
        assertEquals(1, ClientMessageGrouper.filter(
                newClient, "03849", ClientMessageGrouper.Filter.NEW).size());

        List<ClientMessageGrouper.ClientGroup> existingClient = ClientMessageGrouper.group(
                Arrays.asList(
                        SmsMessage.create("038 49 002 47", "récent", 200L, true),
                        SmsMessage.create("038 49 002 47", "ancien", 100L, true)));
        assertEquals(1, ClientMessageGrouper.filter(
                existingClient, "03849", ClientMessageGrouper.Filter.EXISTING).size());
    }

    @Test public void filter_usesCurrentMessageCountAndPreservesRecencyOrder() {
        List<ClientMessageGrouper.ClientGroup> groups = ClientMessageGrouper.group(Arrays.asList(
                SmsMessage.create("new", "unique", 200L, true),
                SmsMessage.create("old-recent", "récent", 300L, true),
                SmsMessage.create("old-recent", "ancien", 100L, true),
                SmsMessage.create("old-older", "récent", 250L, true),
                SmsMessage.create("old-older", "ancien", 50L, true)));

        List<ClientMessageGrouper.ClientGroup> newClients = ClientMessageGrouper.filter(groups, "",
                ClientMessageGrouper.Filter.NEW);
        assertEquals(1, newClients.size());
        assertEquals("new", newClients.get(0).sender);

        List<ClientMessageGrouper.ClientGroup> existingClients = ClientMessageGrouper.filter(
                groups, "old", ClientMessageGrouper.Filter.EXISTING);
        assertEquals(Arrays.asList("old-recent", "old-older"), Arrays.asList(
                existingClients.get(0).sender, existingClients.get(1).sender));
    }

    @Test public void filter_movesClientFromNewToExistingAfterSecondMessage() {
        SmsMessage first = SmsMessage.create("0344153878", "premier", 100L, true);
        assertEquals(1, ClientMessageGrouper.filter(
                ClientMessageGrouper.group(Collections.singletonList(first)), "",
                ClientMessageGrouper.Filter.NEW).size());

        SmsMessage second = SmsMessage.create("0344153878", "deuxième", 200L, true);
        List<ClientMessageGrouper.ClientGroup> regrouped = ClientMessageGrouper.group(
                Arrays.asList(first, second));
        assertEquals(0, ClientMessageGrouper.filter(regrouped, "",
                ClientMessageGrouper.Filter.NEW).size());
        assertEquals(1, ClientMessageGrouper.filter(regrouped, "",
                ClientMessageGrouper.Filter.EXISTING).size());
    }
}
