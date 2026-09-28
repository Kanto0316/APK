package com.netk.mvolatrack;

import android.app.Application;
import androidx.lifecycle.ProcessLifecycleOwner;
import com.netk.mvolatrack.integrity.AppIntegrityChecker;
import com.netk.mvolatrack.security.AppLockManager;

public class MvolaApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        // Run independently at process startup; activities enforce the result before access.
        AppIntegrityChecker.check(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new AppLockManager(this));
    }
}
