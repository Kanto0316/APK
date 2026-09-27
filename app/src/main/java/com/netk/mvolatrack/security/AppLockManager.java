package com.netk.mvolatrack.security;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public final class AppLockManager implements DefaultLifecycleObserver {
    private static boolean unlocked;
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
    public static void showLockIfRequired(Activity activity) {
        SecurityStore store = new SecurityStore(activity);
        if (store.isLockEnabled() && !unlocked && !(activity instanceof LockActivity)) {
            Intent intent = new Intent(activity, LockActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            activity.startActivity(intent);
        }
    }
    public static void markUnlocked() { unlocked = true; }
    public static void markLocked() { unlocked = false; }
}
