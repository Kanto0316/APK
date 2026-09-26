package com.netk.mvolatrack.sms;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AuthorizedSmsSendersTest {
    @Test public void acceptsExactSenderIgnoringCaseAndOuterWhitespace() {
        assertTrue(AuthorizedSmsSenders.isAuthorized(" MVOLA "));
        assertTrue(AuthorizedSmsSenders.isAuthorized("mVoLa"));
    }

    @Test public void rejectsMissingOrLookalikeSenders() {
        assertFalse(AuthorizedSmsSenders.isAuthorized(null));
        assertFalse(AuthorizedSmsSenders.isAuthorized(""));
        assertFalse(AuthorizedSmsSenders.isAuthorized("Info MVola"));
        assertFalse(AuthorizedSmsSenders.isAuthorized("MVolaCash"));
    }
}
