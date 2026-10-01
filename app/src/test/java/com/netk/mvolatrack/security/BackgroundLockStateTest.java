package com.netk.mvolatrack.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class BackgroundLockStateTest {
    @Test public void coldStartRequiresAuthentication() {
        BackgroundLockState state = new BackgroundLockState();
        assertTrue(state.activityStarted(10L));
        assertTrue(state.timeoutReached(SecurityStore.FIVE_MINUTES, 10L));
    }

    @Test public void checksAllSupportedTimeoutsAtTheirBoundary() {
        long[] timeouts = {SecurityStore.IMMEDIATE, SecurityStore.ONE_MINUTE,
                SecurityStore.FIVE_MINUTES};
        for (long timeout : timeouts) {
            BackgroundLockState state = backgroundedAt(1_000L);
            assertTrue(state.activityStarted(1_000L + timeout));
            assertTrue(state.timeoutReached(timeout, 1_000L + timeout));
        }
    }

    @Test public void returningBeforeTimeoutDoesNotLock() {
        BackgroundLockState state = backgroundedAt(1_000L);
        assertTrue(state.activityStarted(60_999L));
        assertFalse(state.timeoutReached(SecurityStore.ONE_MINUTE, 60_999L));
    }

    @Test public void internalNavigationDoesNotStartBackgroundTimer() {
        BackgroundLockState state = new BackgroundLockState();
        assertTrue(state.activityStarted(0L));
        assertTrue(state.timeoutReached(0L, 0L));
        assertFalse(state.activityStarted(100L));
        state.activityStopped(false, 200L);
        assertFalse(state.activityStarted(300L));
    }

    @Test public void configurationChangeDoesNotCountAsBackground() {
        BackgroundLockState state = new BackgroundLockState();
        state.activityStarted(0L);
        state.timeoutReached(0L, 0L);
        state.activityStopped(true, 10L);
        assertFalse(state.activityStarted(400_000L));
    }

    private static BackgroundLockState backgroundedAt(long time) {
        BackgroundLockState state = new BackgroundLockState();
        state.activityStarted(0L);
        state.timeoutReached(0L, 0L);
        state.activityStopped(false, time);
        return state;
    }
}
