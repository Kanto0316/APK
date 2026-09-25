package com.netk.mvolatrack;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OfferUssdTest {
    @Test public void buildsOfferCodeFromFormattedMalagasyNumber() {
        assertEquals("#111*1*4*5*0341234567#",
                OfferUssd.buildUssdCode("034 12 345 67"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidRecipientNumber() {
        OfferUssd.buildUssdCode("VARIABLE_NUMERO");
    }
}
