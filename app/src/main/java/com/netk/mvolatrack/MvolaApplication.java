package com.netk.mvolatrack;

import android.app.Application;
import androidx.lifecycle.ProcessLifecycleOwner;
import com.netk.mvolatrack.security.AppLockManager;

public class MvolaApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        CrashLogger.initialize(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new AppLockManager(this));
    }
}
