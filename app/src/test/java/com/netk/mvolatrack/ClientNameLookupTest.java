package com.netk.mvolatrack;

import static org.junit.Assert.assertEquals;

import com.netk.mvolatrack.database.SmsMessage;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class ClientNameLookupTest {
    private static SmsMessage message(long id, long date, String body) {
        SmsMessage message = SmsMessage.create("MVola", body, date, true);
        message.id = id;
        return message;
    }

    @Test
    public void findsKnownNameForFormattedPhone() {
        SmsMessage known = message(1, 100,
                "Vous avez crédité MARIETTE (0341411058) de 300 Ar. Ref: 1");

        assertEquals("MARIETTE", ClientNameLookup.findClientNameByPhone(
                Collections.singletonList(known), "034 14 110 58"));
    }

    @Test
    public void unknownPhoneUsesVisibleFallback() {
        SmsMessage known = message(1, 100,
                "Vous avez crédité MARIETTE (0341411058) de 300 Ar. Ref: 1");

        assertEquals("-", ClientNameLookup.findClientNameByPhone(
                Collections.singletonList(known), "0349999999"));
    }

    @Test
    public void usesMostRecentNonEmptyNameRegardlessOfInputOrder() {
        SmsMessage older = message(1, 100,
                "Vous avez crédité ANCIEN NOM (0341411058) de 300 Ar. Ref: 1");
        SmsMessage newestWithoutName = message(3, 300,
                "Achat de crédit YAS réussi: 300 Ar pour 0341411058. Ref: 3");
        SmsMessage newestNamed = message(2, 200,
                "Vous avez crédité MARIETTE (034 14 110 58) de 300 Ar. Ref: 2");

        assertEquals("MARIETTE", ClientNameLookup.findClientNameByPhone(
                Arrays.asList(older, newestWithoutName, newestNamed), "0341411058"));
    }
}
