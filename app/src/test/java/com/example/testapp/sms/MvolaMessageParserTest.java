package com.example.testapp.sms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class MvolaMessageParserTest {
    private static final String EXAMPLE_A = "11 000 Ar recu de Mirado Rodhino 0381453472 "
            + "le 21/09/26 a 08:31. Bonus:250 Ar. Solde: 34 800 Ar. Ref: 7586649044.";
    private static final String EXAMPLE_B = "3 000 Ar recu de LOVASOA 0343242318 "
            + "le 21/09/26 a 08:19. Bonus:58 Ar. Solde: 82 023 Ar. Ref: 7582999265.";
    private static final String CREDIT_A = "Achat de credit YAS reussi: 500 Ar pour 0341444033. "
            + "Frais: 0 Ar. Bonus:24 Ar. Solde MVola : 88 965 Ar. Ref: 7586367275";
    private static final String CREDIT_B = "Achat de credit YAS reussi: 500 Ar pour 0386825677. "
            + "Frais: 0 Ar. Bonus:24 Ar. Solde MVola : 80 869 Ar. Ref: 7581687806";

    @Test public void parsesProvidedExampleA() { assertTransaction(EXAMPLE_A, "0381453472",
            "Mirado Rodhino", 11000, 250, 34800, "7586649044", "21/09/2026 08:31"); }

    @Test public void parsesProvidedExampleB() { assertTransaction(EXAMPLE_B, "0343242318",
            "LOVASOA", 3000, 58, 82023, "7582999265", "21/09/2026 08:19"); }

    @Test public void acceptsAccentsCaseSpacingAndMissingOptionalFields() {
        MvolaMessageParser.ParsedTransaction parsed = MvolaMessageParser.parse(
                "1 500 Ar Reçu   de Jean Test +261 38 145 3472 le 1/2/2026 à 7:05. rEf:42.");
        assertEquals("0381453472", parsed.clientNumber);
        assertEquals(1500, parsed.amount);
        assertEquals("42", parsed.reference);
        assertNull(parsed.bonus);
        assertNull(parsed.fee);
        assertNull(parsed.balance);
    }

    @Test public void normalizesEquivalentMalagasyNumbersToOneKey() {
        assertEquals("0381453472", ClientNumberNormalizer.normalize("0381453472"));
        assertEquals("0381453472", ClientNumberNormalizer.normalize("038 14 534 72"));
        assertEquals("0381453472", ClientNumberNormalizer.normalize("+261381453472"));
        assertEquals("038 14 534 72", ClientNumberNormalizer.format("0381453472"));
    }

    @Test public void parsesProvidedCreditExampleAUsingReceptionTime() {
        assertCredit(CREDIT_A, "0341444033", 88965, "7586367275");
    }

    @Test public void parsesProvidedCreditExampleBUsingReceptionTime() {
        assertCredit(CREDIT_B, "0386825677", 80869, "7581687806");
    }

    @Test public void creditRecognitionToleratesAccentsCaseAndSpacing() {
        MvolaMessageParser.ParsedTransaction parsed = MvolaMessageParser.parse(
                "  ACHAT   DE CRÉDIT YAS RÉUSSI : 500 Ar pour 034 14 440 33. "
                        + "FRAIS : 0 Ar. BONUS : 24 Ar. Solde MVola: 88 965 Ar. Réf : 42", 9L);
        assertEquals("Crédit", parsed.type);
        assertEquals("0341444033", parsed.clientNumber);
        assertEquals(9L, parsed.transactionAt);
    }

    private static void assertCredit(String message, String number, long balance,
                                     String reference) {
        MvolaMessageParser.ParsedTransaction parsed = MvolaMessageParser.parse(message, 123456L);
        assertEquals("Crédit", parsed.type);
        assertEquals(number, parsed.clientNumber);
        assertEquals("-", parsed.clientName);
        assertEquals(500L, parsed.amount);
        assertEquals(Long.valueOf(0L), parsed.fee);
        assertEquals(Long.valueOf(24L), parsed.bonus);
        assertEquals(Long.valueOf(balance), parsed.balance);
        assertEquals(reference, parsed.reference);
        assertEquals(123456L, parsed.transactionAt);
        assertEquals(message, parsed.rawMessage);
    }

    private static void assertTransaction(String message, String number, String name, long amount,
                                          long bonus, long balance, String reference, String date) {
        TimeZone previous = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        try {
            MvolaMessageParser.ParsedTransaction parsed = MvolaMessageParser.parse(message);
            assertEquals("Retrait", parsed.type);
            assertEquals(number, parsed.clientNumber);
            assertEquals(name, parsed.clientName);
            assertEquals(amount, parsed.amount);
            assertEquals(Long.valueOf(bonus), parsed.bonus);
            assertNull(parsed.fee);
            assertEquals(Long.valueOf(balance), parsed.balance);
            assertEquals(reference, parsed.reference);
            assertEquals(date, new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH)
                    .format(parsed.transactionAt));
            assertEquals(message, parsed.rawMessage);
        } finally {
            TimeZone.setDefault(previous);
        }
    }
}
