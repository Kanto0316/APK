package com.example.testapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.testapp.database.SmsMessage;

import org.junit.Test;


public class SmsTableRowTest {
    @Test
    public void from_rejectsUnparsedSmsInsteadOfDisplayingItsSender() {
        SmsMessage message = SmsMessage.create("MVola", "Texte intégral conservé", 1L, true);
        try {
            SmsTableRow.from(new SmsDateFilter.DisplayMessage(message, 15));
        } catch (IllegalArgumentException expected) {
            assertEquals("A parsed transaction is required for display", expected.getMessage());
            assertEquals("Texte intégral conservé", message.messageBody);
            assertEquals("MVola", message.sender);
            return;
        }
        throw new AssertionError("An unparsed SMS must not become a table row");
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
