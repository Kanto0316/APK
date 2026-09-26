package com.netk.mvolatrack.sms;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Single source of truth for SMS senders that may create MVola data. */
public final class AuthorizedSmsSenders {
    private static final Set<String> WHITELIST;

    static {
        Set<String> senders = new HashSet<>();
        senders.add("mvola");
        WHITELIST = Collections.unmodifiableSet(senders);
    }

    private AuthorizedSmsSenders() {}

    /** Matches the entire trimmed sender, case-insensitively. */
    public static boolean isAuthorized(String sender) {
        return sender != null && WHITELIST.contains(sender.trim().toLowerCase(Locale.ROOT));
    }
}
