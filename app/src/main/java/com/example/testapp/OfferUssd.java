package com.example.testapp;

/** Builds the USSD request used by the offer workflow. */
final class OfferUssd {
    private OfferUssd() {}

    static String buildUssdCode(String recipientNumber) {
        String recipient = DepositUssd.normalizeRecipientNumber(recipientNumber);
        if (recipient == null) {
            throw new IllegalArgumentException("Validated recipient number required");
        }
        return "#111*1*4*5*" + recipient + "#";
    }
}
