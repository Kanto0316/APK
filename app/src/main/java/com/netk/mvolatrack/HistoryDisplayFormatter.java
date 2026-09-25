package com.netk.mvolatrack;

import com.netk.mvolatrack.history.HistoryTransaction;

import java.text.NumberFormat;
import java.util.Locale;

/** Pure formatting rules for history cards. */
final class HistoryDisplayFormatter {
    enum Direction { NORTH_EAST, SOUTH_EAST }

    private HistoryDisplayFormatter() {}

    static String number(String clientNumber) {
        return SmsDisplayFormatter.sender(clientNumber);
    }

    static String reference(String reference) {
        return "Réf " + (reference == null || reference.trim().isEmpty() ? "-" : reference);
    }

    static String bonus(Long bonus) {
        return bonus == null ? "-" : ariary(bonus);
    }

    static String amount(HistoryTransaction item) {
        // Direction comes only from the parser's explicit type; Retrait is an incoming agent
        // transaction in the existing MVola parser.
        if ("Crédit".equalsIgnoreCase(item.type)
                || "Dépôt".equalsIgnoreCase(item.type)) {
            return "-" + ariary(Math.abs(item.amount));
        }
        boolean outgoing = "Envoi".equalsIgnoreCase(item.type)
                || "Paiement".equalsIgnoreCase(item.type)
                || "Transfert".equalsIgnoreCase(item.type);
        return (outgoing ? "-" : "+") + ariary(Math.abs(item.amount));
    }

    static Direction direction(HistoryTransaction item) {
        return "Dépôt".equalsIgnoreCase(item.type)
                || "Crédit".equalsIgnoreCase(item.type)
                ? Direction.SOUTH_EAST : Direction.NORTH_EAST;
    }

    private static String ariary(long value) {
        return NumberFormat.getIntegerInstance(Locale.FRENCH).format(value)
                .replace('\u202f', ' ').replace('\u00a0', ' ') + " Ar";
    }
}
