package com.netk.mvolatrack.security;

import org.junit.Test;
import static org.junit.Assert.*;

public class PinHasherTest {
    @Test public void hashCanVerifyWithoutStoringPin() {
        byte[] salt = PinHasher.newSalt();
        byte[] hash = PinHasher.hash("1234".toCharArray(), salt, PinHasher.ITERATIONS);
        assertTrue(PinHasher.verify("1234".toCharArray(), salt, hash, PinHasher.ITERATIONS));
        assertFalse(PinHasher.verify("4321".toCharArray(), salt, hash, PinHasher.ITERATIONS));
    }
    @Test public void saltsProduceDifferentHashes() {
        byte[] first = PinHasher.hash("1234".toCharArray(), PinHasher.newSalt(), PinHasher.ITERATIONS);
        byte[] second = PinHasher.hash("1234".toCharArray(), PinHasher.newSalt(), PinHasher.ITERATIONS);
        assertFalse(java.util.Arrays.equals(first, second));
    }

    @Test public void emptyPinIsRejectedWithoutHashing() {
        byte[] salt = PinHasher.newSalt();
        byte[] hash = PinHasher.hash("1234".toCharArray(), salt, PinHasher.ITERATIONS);
        char[] emptyPin = new char[0];

        assertFalse(PinHasher.verify(emptyPin, salt, hash, PinHasher.ITERATIONS));
        assertEquals(0, emptyPin.length);
    }

    @Test public void incompleteAndIncorrectPinsAreRejectedAndCorrectPinIsAccepted() {
        byte[] salt = PinHasher.newSalt();
        byte[] hash = PinHasher.hash("1234".toCharArray(), salt, PinHasher.ITERATIONS);

        for (String incomplete : new String[] {"1", "12", "123"}) {
            assertFalse(PinHasher.verify(incomplete.toCharArray(), salt, hash, PinHasher.ITERATIONS));
        }
        assertFalse(PinHasher.verify("4321".toCharArray(), salt, hash, PinHasher.ITERATIONS));
        assertTrue(PinHasher.verify("1234".toCharArray(), salt, hash, PinHasher.ITERATIONS));
    }
}
