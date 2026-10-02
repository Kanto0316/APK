package com.netk.mvolatrack.sim;

/** Produces the single, non-interactive SIM status line shown in the navigation drawer. */
public final class SimStatusLabel {
    private SimStatusLabel() { }

    public static String from(SimOperatorVerifier.Result result) {
        switch (result.state) {
            case DETECTED:
                return "État de SIM : OK (" + OperatorConfiguration.EXPECTED_OPERATOR_NAME + ")";
            case NOT_DETECTED:
                return "État de SIM : Autre opérateur";
            case NO_SIM:
                return "État de SIM : Aucune SIM";
            default:
                return "État de SIM : Vérification impossible";
        }
    }
}
