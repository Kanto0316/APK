package com.netk.mvolatrack.sim;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/** Pure comparison logic kept independent from Android's telephony APIs for testability. */
public final class SimOperatorVerifier {
    public enum State { DETECTED, NOT_DETECTED, NO_SIM, IMPOSSIBLE }

    public static final class SimIdentity {
        public final String carrierName;
        public final String networkId;
        public final int slotIndex;

        public SimIdentity(String carrierName, String networkId, int slotIndex) {
            this.carrierName = carrierName == null ? "" : carrierName;
            this.networkId = networkId == null ? "" : networkId;
            this.slotIndex = slotIndex;
        }
    }

    public static final class Result {
        public final State state;
        public final int slotIndex;

        private Result(State state, int slotIndex) {
            this.state = state;
            this.slotIndex = slotIndex;
        }
    }

    private SimOperatorVerifier() {}

    public static Result verify(List<SimIdentity> activeSims, boolean permissionGranted) {
        if (!permissionGranted || activeSims == null) return new Result(State.IMPOSSIBLE, -1);
        if (activeSims.isEmpty()) return new Result(State.NO_SIM, -1);

        String expectedName = normalize(OperatorConfiguration.EXPECTED_OPERATOR_NAME);
        boolean hasUsableInformation = false;
        for (SimIdentity sim : activeSims) {
            String carrier = normalize(sim.carrierName);
            if (!carrier.isEmpty()) hasUsableInformation = true;
            if (expectedName.equals(carrier)) {
                return new Result(State.DETECTED, sim.slotIndex);
            }
        }
        return new Result(hasUsableInformation ? State.NOT_DETECTED : State.IMPOSSIBLE, -1);
    }

    static String normalize(String value) {
        if (value == null) return "";
        String noAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return noAccents.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

}
