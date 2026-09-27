package com.netk.mvolatrack.security;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public final class AppLockManager implements DefaultLifecycleObserver {
    private static boolean unlocked;
    // MainActivity and SecurityActivity can both reach onCreate/onResume before the
    // first LockActivity is displayed. Keep the launch decision here so those
    // callbacks cannot stack several lock screens.
    private static boolean lockActivityRequested;
    private static long backgroundAt = -1L;
    private final SecurityStore store;
    public AppLockManager(Context context) { store = new SecurityStore(context); }

    @Override public void onStop(LifecycleOwner owner) {
        if (store.isLockEnabled()) backgroundAt = SystemClock.elapsedRealtime();
    }
    @Override public void onStart(LifecycleOwner owner) {
        if (!store.isLockEnabled()) return;
        if (backgroundAt < 0 || SystemClock.elapsedRealtime() - backgroundAt >= store.getTimeout())
            unlocked = false;
        backgroundAt = -1L;
    }
    public static synchronized void showLockIfRequired(Activity activity) {
        SecurityStore store = new SecurityStore(activity);
        if (!store.isLockEnabled()) {
            lockActivityRequested = false;
            return;
        }
        if (unlocked || lockActivityRequested || activity instanceof LockActivity) return;

        lockActivityRequested = true;
        Intent intent = new Intent(activity, LockActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivity(intent);
    }
    public static synchronized void markUnlocked() {
        unlocked = true;
        lockActivityRequested = false;
    }
    public static synchronized void markLocked() { unlocked = false; }
}
