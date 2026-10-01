package com.netk.mvolatrack;

import android.app.Application;
import com.netk.mvolatrack.security.AppLockManager;

public class MvolaApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        CrashLogger.initialize(this);
        registerActivityLifecycleCallbacks(new AppLockManager(this));
    }
}
