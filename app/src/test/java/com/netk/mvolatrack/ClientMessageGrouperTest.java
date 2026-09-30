package com.netk.mvolatrack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.netk.mvolatrack.database.SmsMessage;
import org.junit.Test;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

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

    @Test public void filtersUseFirstTransactionDayRatherThanTransactionCount() {
        TimeZone zone = TimeZone.getTimeZone("Indian/Antananarivo");
        List<ClientMessageGrouper.ClientGroup> groups = ClientMessageGrouper.group(Arrays.asList(
                datedTransaction("0343242318", "premier", 2026, 9, 29, 8, zone),
                datedTransaction("0343242318", "deuxième", 2026, 9, 30, 8, zone),
                datedTransaction("0384900247", "unique", 2026, 9, 30, 9, zone),
                SmsMessage.create("MVola", "ignoré", 400L, true)));
        long september30 = time(2026, 9, 30, 12, zone);

        assertEquals(1, ClientMessageGrouper.filter(groups, "",
                ClientMessageGrouper.Filter.NEW, september30, zone).size());
        assertEquals("0384900247", ClientMessageGrouper.filter(groups, "",
                ClientMessageGrouper.Filter.NEW, september30, zone).get(0).sender);
        assertEquals(1, ClientMessageGrouper.filter(groups, "",
                ClientMessageGrouper.Filter.EXISTING, september30, zone).size());

        // The category is derived from the current day, so no persisted flag needs updating.
        long october1 = time(2026, 10, 1, 0, zone);
        assertEquals(0, ClientMessageGrouper.filter(groups, "",
                ClientMessageGrouper.Filter.NEW, october1, zone).size());
        assertEquals(2, ClientMessageGrouper.filter(groups, "",
                ClientMessageGrouper.Filter.EXISTING, october1, zone).size());
    }

    private static SmsMessage transaction(String number, String label, long receivedAt) {
        return SmsMessage.create("MVola", "1 000 Ar recu de " + label + " " + number
                + " le 21/09/26 a 08:19. Bonus:1 Ar. Solde: 10 000 Ar. Ref: 123456.",
                receivedAt, true);
    }

    private static SmsMessage datedTransaction(String number, String label, int year, int month,
                                                int day, int hour, TimeZone zone) {
        long timestamp = time(year, month, day, hour, zone);
        String date = String.format(java.util.Locale.ROOT, "%02d/%02d/%02d a %02d:00",
                day, month, year % 100, hour);
        return SmsMessage.create("MVola", "1 000 Ar recu de " + label + " " + number
                + " le " + date + ". Bonus:1 Ar. Solde: 10 000 Ar. Ref: 123456.",
                timestamp, true);
    }

    private static long time(int year, int month, int day, int hour, TimeZone zone) {
        Calendar calendar = Calendar.getInstance(zone);
        calendar.clear();
        calendar.set(year, month - 1, day, hour, 0);
        return calendar.getTimeInMillis();
    }
}
