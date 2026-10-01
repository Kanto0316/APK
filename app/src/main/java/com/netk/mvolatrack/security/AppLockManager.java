package com.netk.mvolatrack.security;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;

import com.netk.mvolatrack.activation.ActivationActivity;

/**
 * Application-wide gate for the app lock.
 *
 * <p>This deliberately follows Activities rather than individual screens.  The first Activity
 * started after the task has gone into the background is covered by {@link LockActivity}, before
 * it gets to onResume. Internal Activity transitions never start the background timer.</p>
 */
public final class AppLockManager implements Application.ActivityLifecycleCallbacks {
    private static final Object LOCK = new Object();
    private static boolean unlocked;
    private static boolean lockActivityRequested;

    private final SecurityStore store;
    private final BackgroundLockState state = new BackgroundLockState();

    public AppLockManager(Application application) {
        store = new SecurityStore(application);
    }

    @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (activity instanceof LockActivity) {
            synchronized (LOCK) { lockActivityRequested = true; }
        }
    }

    @Override public void onActivityStarted(Activity activity) {
        boolean enteredForeground = state.activityStarted(SystemClock.elapsedRealtime());
        if (enteredForeground) {
            synchronized (LOCK) {
                if (!store.isLockEnabled()) {
                    unlocked = true;
                    lockActivityRequested = false;
                } else if (state.timeoutReached(store.getTimeout(), SystemClock.elapsedRealtime())) {
                    unlocked = false;
                }
            }
        }
        // Also check every newly opened internal Activity. This covers the hand-off from the
        // licence Activity at cold start without treating that hand-off as background time.
        showLockIfRequired(activity);
    }

    @Override public void onActivityStopped(Activity activity) {
        state.activityStopped(activity.isChangingConfigurations(), SystemClock.elapsedRealtime());
    }

    @Override public void onActivityDestroyed(Activity activity) {
        if (activity instanceof LockActivity && !activity.isChangingConfigurations()) {
            synchronized (LOCK) { lockActivityRequested = false; }
        }
    }

    private static void showLockIfRequired(Activity activity) {
        // Activation must remain reachable. Once it opens MainActivity, the normal gate applies.
        if (activity instanceof ActivationActivity || activity instanceof LockActivity
                || activity.isFinishing()) return;

        SecurityStore store = new SecurityStore(activity);
        synchronized (LOCK) {
            if (!store.isLockEnabled()) {
                unlocked = true;
                lockActivityRequested = false;
                return;
            }
            if (unlocked || lockActivityRequested) return;
            lockActivityRequested = true;
        }

        Intent intent = new Intent(activity, LockActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }

    public static void markUnlocked() {
        synchronized (LOCK) {
            unlocked = true;
            lockActivityRequested = false;
        }
    }

    public static void markLocked() {
        synchronized (LOCK) { unlocked = false; }
    }

    @Override public void onActivityResumed(Activity activity) {}
    @Override public void onActivityPaused(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
}
