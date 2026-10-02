package com.netk.mvolatrack.sim;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public final class SimStatusLabelTest {
    @Test public void rendersEveryStateOnOneLine() {
        assertLabel("État de SIM : OK (Yas)", "Yas", true);
        assertLabel("État de SIM : Autre opérateur", "Telma", true);
        assertLabel("État de SIM : Vérification impossible", "", true);
        assertEquals("État de SIM : Aucune SIM", SimStatusLabel.from(
                SimOperatorVerifier.verify(Collections.emptyList(), true)));
        assertEquals("État de SIM : Vérification impossible", SimStatusLabel.from(
                SimOperatorVerifier.verify(null, false)));
    }

    private static void assertLabel(String expected, String carrier, boolean permission) {
        SimOperatorVerifier.SimIdentity sim =
                new SimOperatorVerifier.SimIdentity(carrier, "", 0);
        String label = SimStatusLabel.from(SimOperatorVerifier.verify(
                Collections.singletonList(sim), permission));
        assertEquals(expected, label);
        assertEquals(-1, label.indexOf('\n'));
    }
}
