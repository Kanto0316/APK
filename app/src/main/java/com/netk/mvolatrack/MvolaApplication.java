package com.netk.mvolatrack;

import android.app.Application;
import com.netk.mvolatrack.security.AppLockManager;
import com.netk.mvolatrack.daily.DailySummaryScheduler;

public class MvolaApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        CrashLogger.initialize(this);
        registerActivityLifecycleCallbacks(new AppLockManager(this));
        DailySummaryScheduler.reconcile(this);
    }
}
