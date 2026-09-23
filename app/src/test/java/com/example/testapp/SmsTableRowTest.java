package com.example.testapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.testapp.database.SmsMessage;

import org.junit.Test;

import java.util.TimeZone;

public class SmsTableRowTest {
    @Test
    public void from_usesReliableSmsFieldsAndLeavesFutureFieldsUnavailable() {
        TimeZone previous = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        try {
            SmsMessage message = SmsMessage.create("+261344153878", "Texte intégral conservé",
                    1790186280000L, true);
            SmsTableRow row = SmsTableRow.from(new SmsDateFilter.DisplayMessage(message, 15));

            assertEquals(15, row.number);
            assertEquals("23/09/2026 17:58", row.dateTime);
            assertEquals("034 41 538 78", row.numero);
            assertNull(row.type);
            assertNull(row.nom);
            assertNull(row.montant);
            assertNull(row.reference);
            assertNull(row.bonus);
            assertNull(row.frais);
            assertNull(row.solde);
            assertEquals("Texte intégral conservé", message.messageBody);
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @Test
    public void from_usesParsedMvolaTransactionInsteadOfAndroidSenderAndTimestamp() {
        SmsMessage message = SmsMessage.create("MVola", "11 000 Ar recu de Mirado Rodhino "
                + "0381453472 le 21/09/26 a 08:31. Bonus:250 Ar. "
                + "Solde: 34 800 Ar. Ref: 7586649044.", 1L, true);
        SmsTableRow row = SmsTableRow.from(new SmsDateFilter.DisplayMessage(message, 1));
        assertEquals("21/09/2026 08:31", row.dateTime);
        assertEquals("Retrait", row.type);
        assertEquals("038 14 534 72", row.numero);
        assertEquals("Mirado Rodhino", row.nom);
        assertEquals("11 000 Ar", row.montant);
        assertEquals("7586649044", row.reference);
        assertEquals("250 Ar", row.bonus);
        assertNull(row.frais);
        assertEquals("34 800 Ar", row.solde);
    }

    @Test
    public void display_rendersOnlyDashForUnavailableValues() {
        assertEquals("-", SmsTableRow.display(null));
        assertEquals("-", SmsTableRow.display(""));
        assertEquals("-", SmsTableRow.display("   "));
        assertEquals("25 000 Ar", SmsTableRow.display("25 000 Ar"));
    }
}
