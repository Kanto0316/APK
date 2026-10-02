package com.netk.mvolatrack.sim;

import org.junit.Test;

import java.util.Collections;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public final class SimOperatorVerifierTest {
    private static SimOperatorVerifier.SimIdentity sim(String name, String network, int slot) {
        return new SimOperatorVerifier.SimIdentity(name, network, slot);
    }

    @Test public void detectsConfiguredOperatorInSimOne() {
        SimOperatorVerifier.Result result = SimOperatorVerifier.verify(
                Collections.singletonList(sim("  YAS ", "", 0)), true);
        assertEquals(SimOperatorVerifier.State.DETECTED, result.state);
        assertEquals(0, result.slotIndex);
    }

    @Test public void examinesEveryActiveSimAndDetectsSimTwo() {
        SimOperatorVerifier.Result result = SimOperatorVerifier.verify(Arrays.asList(
                sim("Autre réseau", "00101", 0), sim("Yas", "00202", 1)), true);
        assertEquals(SimOperatorVerifier.State.DETECTED, result.state);
        assertEquals(1, result.slotIndex);
    }

    @Test public void reportsDifferentOperatorAsNotDetected() {
        assertEquals(SimOperatorVerifier.State.NOT_DETECTED,
                SimOperatorVerifier.verify(
                        Collections.singletonList(sim("Autre réseau", "00101", 0)), true).state);
    }

    @Test public void reportsDeniedPermissionAsImpossible() {
        assertEquals(SimOperatorVerifier.State.IMPOSSIBLE,
                SimOperatorVerifier.verify(null, false).state);
    }

    @Test public void reportsNoActiveSim() {
        assertEquals(SimOperatorVerifier.State.NO_SIM,
                SimOperatorVerifier.verify(Collections.emptyList(), true).state);
    }

    @Test public void reportsLockedOrUnreliableSimAsImpossible() {
        assertEquals(SimOperatorVerifier.State.IMPOSSIBLE,
                SimOperatorVerifier.verify(
                        Collections.singletonList(sim("", "", 0)), true).state);
        assertEquals(SimOperatorVerifier.State.IMPOSSIBLE,
                SimOperatorVerifier.verify(null, true).state);
    }

    @Test public void reflectsSimReplacementOnNextVerification() {
        assertEquals(SimOperatorVerifier.State.DETECTED,
                SimOperatorVerifier.verify(
                        Collections.singletonList(sim("Yas", "", 0)), true).state);
        assertEquals(SimOperatorVerifier.State.NOT_DETECTED,
                SimOperatorVerifier.verify(
                        Collections.singletonList(sim("Nouveau réseau", "", 0)), true).state);
    }

    @Test public void rejectsAliasesTelmaAndPhoneNumbers() {
        assertEquals(SimOperatorVerifier.State.NOT_DETECTED,
                SimOperatorVerifier.verify(Arrays.asList(
                        sim("Telma", "64601", 0), sim("0340000000", "", 1)), true).state);
    }
}
