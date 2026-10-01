package com.netk.mvolatrack.daily;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.netk.mvolatrack.database.AppDatabase;
import com.netk.mvolatrack.database.DailySummaryDao;
import com.netk.mvolatrack.database.DailySummaryReport;
import com.netk.mvolatrack.database.NotificationBonus;
import com.netk.mvolatrack.database.NotificationType;
import java.util.concurrent.Executors;

public final class DailySummarySentReceiver extends BroadcastReceiver {
    public static final String ACTION="com.netk.mvolatrack.DAILY_SUMMARY_SENT";
    @Override public void onReceive(Context context,Intent intent){
        PendingResult pending=goAsync(); long id=intent.getLongExtra("report_id",-1);
        // goAsync() transfers the ordered-broadcast result to PendingResult. Reading it from
        // BroadcastReceiver after that point returns the receiver's reset/default value (0).
        int result=pending.getResultCode();
        Integer modemError=intent.hasExtra("errorCode")?intent.getIntExtra("errorCode",0):null;
        Executors.newSingleThreadExecutor().execute(()->{ try{
            DailySummaryDao dao=AppDatabase.getInstance(context).dailySummaryDao();
            if(result==Activity.RESULT_OK){
                dao.partSucceeded(id); DailySummaryReport report=dao.get(id);
                if(report!=null && SendResultPolicy.allSegmentsAccepted(
                        report.segmentCount,report.successfulParts)
                        && dao.finishSent(id,System.currentTimeMillis())==1)
                    notify(context,id,true,null);
            } else {String reason=SendResultPolicy.failureReason(result,modemError);
                if(dao.finishFailed(id,reason,System.currentTimeMillis())==1) notify(context,id,false,reason);}
        } finally {pending.finish();}});
    }
    private static void notify(Context context,long id,boolean success,String reason){
        long now=System.currentTimeMillis();String title=success?"Résumé quotidien envoyé":"Échec du résumé quotidien";
        String message=success?"Le SMS a été accepté par Android.":reason;
        AppDatabase.getInstance(context).notificationBonusDao().insertAndTrim(
                NotificationBonus.general(now,"daily-summary-"+id+"-"+success,
                        NotificationType.SYSTEME,title,message,"daily_summary:"+id));
    }
}
