package com.netk.mvolatrack.sms;

import com.netk.mvolatrack.database.Transaction;
import org.junit.Test;
import static org.junit.Assert.*;

public class SmsParserTest {
    private final SmsParser parser = new SmsParser();

    @Test public void parsesBalanceWithSpacesAndAriary() {
        Transaction result = parse("Votre SOLDE est 12 500 Ar");
        assertEquals("SOLDE", result.type);
        assertEquals(12500, result.montant, 0);
        assertEquals(12500, result.solde, 0);
        assertEquals("Ar", result.devise);
    }
    @Test public void parsesBonusWithoutSpacesAndMga() {
        Transaction result = parse("BONUS:2500MGA disponible");
        assertEquals("BONUS", result.type);
        assertEquals(2500, result.bonus, 0);
        assertEquals("MGA", result.devise);
    }
    @Test public void parsesRecharge() { assertEquals(5000, parse("RECHARGE de 5 000 Ar effectuée").montant, 0); }
    @Test public void parsesPaymentWithDecimalAmount() { assertEquals(1250.50, parse("Paiement 1 250,50 MGA accepté").montant, 0); }
    @Test public void parsesTransactionWithCurrencyBeforeAmount() { assertEquals(9000, parse("Transaction Ar 9000 confirmée").montant, 0); }
    @Test public void retainsSenderDateAndOriginalMessage() {
        Transaction result = parser.parse("Solde 100 Ar", "TELCO", 1234L);
        assertNotNull(result);
        assertEquals("TELCO", result.expediteur);
        assertEquals(1234L, result.date);
        assertEquals("Solde 100 Ar", result.messageOriginal);
    }
    private Transaction parse(String message) {
        Transaction result = parser.parse(message, "TELCO", 1234L);
        assertNotNull(result);
        return result;
    }
}
