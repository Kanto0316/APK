package com.netk.mvolatrack.activation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Presentation-only formatting; V2 protocol timestamps remain in epoch seconds. */
public final class LicenseDisplay {
    private LicenseDisplay() { }

    public static String type(ActivationResponse license) {
        if (license == null) return "Inconnue";
        switch (license.getType()) {
            case WEEK: return "1 semaine";
            case MONTH: return "1 mois";
            default: return "Permanente";
        }
    }

    public static String expiry(ActivationResponse license) {
        if (license == null || !license.isTemporary()) return "—";
        return new SimpleDateFormat("dd/MM/yyyy 'à' HH:mm", Locale.getDefault())
                .format(new Date(Math.multiplyExact(license.getExpiresAt(), 1000L)));
    }
}
