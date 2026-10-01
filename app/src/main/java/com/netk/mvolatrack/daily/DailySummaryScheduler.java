package com.netk.mvolatrack.daily;

import android.content.Context;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public final class DailySummaryScheduler {
    private static final String WORK="daily-summary-send";
    private DailySummaryScheduler(){}
    public static void reconcile(Context context){
        DailySummaryPreferences p=new DailySummaryPreferences(context);
        WorkManager wm=WorkManager.getInstance(context);
        if(!p.enabled()){ wm.cancelUniqueWork(WORK); return; }
        Calendar target=Calendar.getInstance(DailySummaryCalculator.BUSINESS_ZONE);
        target.set(Calendar.HOUR_OF_DAY,p.hour()); target.set(Calendar.MINUTE,p.minute());
        target.set(Calendar.SECOND,0); target.set(Calendar.MILLISECOND,0);
        if(target.getTimeInMillis()<=System.currentTimeMillis()) target.add(Calendar.DAY_OF_MONTH,1);
        OneTimeWorkRequest request=new OneTimeWorkRequest.Builder(DailySummaryWorker.class)
                .setInitialDelay(target.getTimeInMillis()-System.currentTimeMillis(),TimeUnit.MILLISECONDS)
                .build();
        wm.enqueueUniqueWork(WORK,ExistingWorkPolicy.REPLACE,request);
    }
}
