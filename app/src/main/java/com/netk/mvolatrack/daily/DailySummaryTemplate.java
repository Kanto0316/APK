package com.netk.mvolatrack.daily;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The single validated rendering engine shared by preview and sending. */
public final class DailySummaryTemplate {
    public static final String DEFAULT = "MVolaCash - {date}\nTransactions : {transactions}\n"
            + "Dépôts : {depots} ({montant_depots})\nRetraits : {retraits} ({montant_retraits})\n"
            + "Crédits : {credits} ({montant_credits})\nClients : {clients}\n"
            + "Bonus enregistrés : {bonus}";
    public static final String[] VARIABLES = {"date", "transactions", "clients", "depots",
            "retraits", "credits", "montant_depots", "montant_retraits",
            "montant_credits", "bonus"};
    private static final Set<String> ALLOWED = new HashSet<>(Arrays.asList(VARIABLES));
    private static final Pattern TOKEN = Pattern.compile("\\{([^{}]+)\\}");
    private DailySummaryTemplate() {}

    public static String validationError(String template) {
        if (template == null || template.trim().isEmpty()) return "Le modèle ne peut pas être vide.";
        Matcher m = TOKEN.matcher(template);
        while (m.find()) if (!ALLOWED.contains(m.group(1)))
            return "Variable inconnue : {" + m.group(1) + "}.";
        String remaining = m.replaceAll("");
        if (remaining.indexOf('{') >= 0 || remaining.indexOf('}') >= 0)
            return "Une accolade du modèle est incomplète.";
        return null;
    }

    public static String render(String template, DailySummary s) {
        String error = validationError(template);
        if (error != null) throw new IllegalArgumentException(error);
        SimpleDateFormat date = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
        date.setTimeZone(DailySummaryCalculator.BUSINESS_ZONE);
        String out = template;
        out = replace(out, "date", date.format(s.dayStart));
        out = replace(out, "transactions", String.valueOf(s.transactions));
        out = replace(out, "clients", String.valueOf(s.clients));
        out = replace(out, "depots", String.valueOf(s.deposits));
        out = replace(out, "retraits", String.valueOf(s.withdrawals));
        out = replace(out, "credits", String.valueOf(s.credits));
        out = replace(out, "montant_depots", money(s.depositAmount));
        out = replace(out, "montant_retraits", money(s.withdrawalAmount));
        out = replace(out, "montant_credits", money(s.creditAmount));
        out = replace(out, "bonus", money(s.bonus) + (s.bonusPartial ? " (partiel)" : ""));
        return out;
    }

    private static String replace(String value, String key, String replacement) {
        return value.replace("{" + key + "}", replacement);
    }
    public static String money(long amount) {
        return NumberFormat.getIntegerInstance(Locale.FRENCH).format(amount) + " Ar";
    }
}
