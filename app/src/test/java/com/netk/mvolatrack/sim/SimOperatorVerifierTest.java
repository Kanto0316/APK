package com.netk.mvolatrack.sim;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public final class SimOperatorVerifierTest {
    private static OperatorConfiguration configured(String networkId, String... aliases) {
        return new OperatorConfiguration("Opérateur Démo", Arrays.asList(aliases), networkId);
    }

    private static SimOperatorVerifier.SimIdentity sim(String name, String network, int slot) {
        return new SimOperatorVerifier.SimIdentity(name, network, slot);
    }

    @Test public void detectsConfiguredOperatorInSimOne() {
        SimOperatorVerifier.Result result = SimOperatorVerifier.verify(configured(""),
                Collections.singletonList(sim("  OPÉRATEUR   démo ", "", 0)), true);
        assertEquals(SimOperatorVerifier.State.DETECTED, result.state);
        assertEquals(0, result.slotIndex);
    }

    @Test public void examinesEveryActiveSimAndDetectsSimTwo() {
        SimOperatorVerifier.Result result = SimOperatorVerifier.verify(configured(""), Arrays.asList(
                sim("Autre réseau", "00101", 0), sim("Opérateur Démo", "00202", 1)), true);
        assertEquals(SimOperatorVerifier.State.DETECTED, result.state);
        assertEquals(1, result.slotIndex);
    }

    @Test public void reportsDifferentOperatorAsNotDetected() {
        assertEquals(SimOperatorVerifier.State.NOT_DETECTED,
                SimOperatorVerifier.verify(configured(""),
                        Collections.singletonList(sim("Autre réseau", "00101", 0)), true).state);
    }

    @Test public void reportsMissingConfigurationBeforeReadingSim() {
        assertEquals(SimOperatorVerifier.State.NOT_CONFIGURED,
                SimOperatorVerifier.verify(new OperatorConfiguration("", Collections.emptyList(), ""),
                        null, false).state);
    }

    @Test public void reportsDeniedPermissionAsImpossible() {
        assertEquals(SimOperatorVerifier.State.IMPOSSIBLE,
                SimOperatorVerifier.verify(configured(""), null, false).state);
    }

    @Test public void reportsMissingOrLockedSimAsImpossible() {
        assertEquals(SimOperatorVerifier.State.IMPOSSIBLE,
                SimOperatorVerifier.verify(configured(""), Collections.emptyList(), true).state);
        assertEquals(SimOperatorVerifier.State.IMPOSSIBLE,
                SimOperatorVerifier.verify(configured(""),
                        Collections.singletonList(sim("", "", 0)), true).state);
    }

    @Test public void reflectsSimReplacementOnNextVerification() {
        assertEquals(SimOperatorVerifier.State.DETECTED,
                SimOperatorVerifier.verify(configured(""),
                        Collections.singletonList(sim("Opérateur Démo", "", 0)), true).state);
        assertEquals(SimOperatorVerifier.State.NOT_DETECTED,
                SimOperatorVerifier.verify(configured(""),
                        Collections.singletonList(sim("Nouveau réseau", "", 0)), true).state);
    }

    @Test public void acceptsConfiguredAlternativeName() {
        assertEquals(SimOperatorVerifier.State.DETECTED,
                SimOperatorVerifier.verify(configured("", "Ancien nom"),
                        Collections.singletonList(sim("ancien NOM", "", 0)), true).state);
    }

    @Test public void storedNetworkIdWinsAndRejectsConflictingId() {
        assertEquals(SimOperatorVerifier.State.DETECTED,
                SimOperatorVerifier.verify(configured("64601"),
                        Collections.singletonList(sim("Nom renommé", "646-01", 0)), true).state);
        assertEquals(SimOperatorVerifier.State.NOT_DETECTED,
                SimOperatorVerifier.verify(configured("64601"),
                        Collections.singletonList(sim("Opérateur Démo", "64602", 0)), true).state);
    }
}
