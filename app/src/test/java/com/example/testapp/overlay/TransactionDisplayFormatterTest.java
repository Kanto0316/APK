package com.example.testapp.overlay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TransactionDisplayFormatterTest {
    @Test public void formatsTransactionValuesForOverlay() {
        assertEquals("034 58 294 12", TransactionDisplayFormatter.phone("0345829412"));
        assertEquals("+ 11 000 Ar", TransactionDisplayFormatter.amount(11000));
        assertEquals("10 800 Ar", TransactionDisplayFormatter.balance(10800L));
    }

    @Test public void missingValuesNeverExposeNull() {
        assertEquals("-", TransactionDisplayFormatter.text(null));
        assertEquals("-", TransactionDisplayFormatter.text("  "));
        assertEquals("-", TransactionDisplayFormatter.phone(null));
        assertEquals("-", TransactionDisplayFormatter.balance(null));
    }

    @Test public void keepsThousandsSeparatedBySpaces() {
        assertEquals("+ 3 000 Ar", TransactionDisplayFormatter.amount(3000));
        assertEquals("+ 150 000 Ar", TransactionDisplayFormatter.amount(150000));
    }

    @Test public void creditAmountDoesNotLookLikeAnIncomingCashMovement() {
        assertEquals("500 Ar", TransactionDisplayFormatter.amount("Crédit", 500));
        assertEquals("+ 500 Ar", TransactionDisplayFormatter.amount("Retrait", 500));
    }
}
