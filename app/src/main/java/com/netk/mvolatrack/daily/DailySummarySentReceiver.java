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
        int result=getResultCode();
        Executors.newSingleThreadExecutor().execute(()->{ try{
            DailySummaryDao dao=AppDatabase.getInstance(context).dailySummaryDao();
            if(result==Activity.RESULT_OK){
                dao.partSucceeded(id); DailySummaryReport report=dao.get(id);
                if(report!=null && report.successfulParts>=report.segmentCount)
                    { dao.finish(id,"ENVOYE",null,System.currentTimeMillis()); notify(context,id,true,null); }
            } else {String reason="Android a refusé un segment (code "+result+")";dao.finish(id,"ECHEC",reason,System.currentTimeMillis());notify(context,id,false,reason);}
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
