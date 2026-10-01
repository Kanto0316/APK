package com.netk.mvolatrack.security;

/** Package-private, clock-independent state machine used by {@link AppLockManager}. */
final class BackgroundLockState {
    private int startedActivities;
    private long backgroundAt = -1L;
    private boolean configurationChange;

    boolean activityStarted(long now) {
        boolean enteredForeground = startedActivities == 0 && !configurationChange;
        configurationChange = false;
        startedActivities++;
        return enteredForeground;
    }

    void activityStopped(boolean changingConfigurations, long now) {
        if (startedActivities > 0) startedActivities--;
        if (startedActivities == 0) {
            if (changingConfigurations) configurationChange = true;
            else backgroundAt = now;
        }
    }

    boolean timeoutReached(long timeout, long now) {
        if (backgroundAt < 0L) return true; // cold process start
        long elapsed = Math.max(0L, now - backgroundAt);
        backgroundAt = -1L;
        return elapsed >= timeout;
    }
}
