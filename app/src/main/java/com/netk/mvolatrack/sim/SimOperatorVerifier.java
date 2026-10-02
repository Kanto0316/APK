package com.netk.mvolatrack.sim;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Pure comparison logic kept independent from Android's telephony APIs for testability. */
public final class SimOperatorVerifier {
    public enum State { DETECTED, NOT_DETECTED, IMPOSSIBLE, NOT_CONFIGURED }

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

    public static Result verify(OperatorConfiguration configuration,
                                List<SimIdentity> activeSims, boolean permissionGranted) {
        if (!configuration.isConfigured()) return new Result(State.NOT_CONFIGURED, -1);
        if (!permissionGranted || activeSims == null || activeSims.isEmpty()) {
            return new Result(State.IMPOSSIBLE, -1);
        }

        List<String> names = new ArrayList<>();
        names.add(normalize(configuration.expectedName));
        for (String alias : configuration.acceptedNames) names.add(normalize(alias));
        boolean hasUsableInformation = false;
        for (SimIdentity sim : activeSims) {
            String simNetwork = normalizeNetworkId(sim.networkId);
            String expectedNetwork = normalizeNetworkId(configuration.networkId);
            String carrier = normalize(sim.carrierName);
            if (!simNetwork.isEmpty() || !carrier.isEmpty()) hasUsableInformation = true;

            // When both network IDs are available, a conflicting ID must not be hidden by a name.
            if (!expectedNetwork.isEmpty() && !simNetwork.isEmpty()) {
                if (expectedNetwork.equals(simNetwork)) return new Result(State.DETECTED, sim.slotIndex);
                continue;
            }
            if (!carrier.isEmpty() && names.contains(carrier)) {
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

    private static String normalizeNetworkId(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }
}
