package com.netk.mvolatrack.daily;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.netk.mvolatrack.database.AppDatabase;
import com.netk.mvolatrack.database.DailySummaryDao;
import com.netk.mvolatrack.database.DailySummaryReport;
import java.util.ArrayList;

public final class DailySummaryWorker extends Worker {
    public DailySummaryWorker(@NonNull Context context,@NonNull WorkerParameters params){super(context,params);}
    @NonNull @Override public Result doWork(){
        Context c=getApplicationContext();
        try { send(c); return Result.success(); }
        catch(Exception error){ return Result.failure(); }
        finally { DailySummaryScheduler.reconcile(c); }
    }
    private static void send(Context c){
        DailySummaryPreferences p=new DailySummaryPreferences(c); if(!p.enabled()) return;
        long day=DailySummaryCalculator.previousDayStart(System.currentTimeMillis());
        AppDatabase db=AppDatabase.getInstance(c); DailySummaryDao dao=db.dailySummaryDao();
        DailySummaryReport existing=dao.find(day,p.recipient()); if(existing!=null) return;
        DailySummary summary=DailySummaryCalculator.calculate(db.smsDao().getAllNewestFirst(),day);
        String text=DailySummaryTemplate.render(p.template(),summary);
        SmsManager manager=selectManager(c);
        ArrayList<String> parts=manager==null?new ArrayList<>():manager.divideMessage(text);
        DailySummaryReport report=new DailySummaryReport(day,"Indian/Antananarivo",p.recipient(),
                text,System.currentTimeMillis(),null,null,"EN_ATTENTE",null,Math.max(1,parts.size()),0);
        long id=dao.insert(report); if(id<1) return;
        if(ContextCompat.checkSelfPermission(c,Manifest.permission.SEND_SMS)!=PackageManager.PERMISSION_GRANTED){
            dao.finish(id,"ECHEC","Autorisation SEND_SMS refusée",System.currentTimeMillis()); return; }
        TelephonyManager phone=(TelephonyManager)c.getSystemService(Context.TELEPHONY_SERVICE);
        if(phone==null || !phone.isSmsCapable()){ dao.finish(id,"ECHEC","Téléphonie SMS indisponible",System.currentTimeMillis()); return; }
        if(manager==null){ dao.finish(id,"ECHEC","Définissez explicitement la SIM SMS par défaut",System.currentTimeMillis()); return; }
        if(dao.beginAttempt(id,System.currentTimeMillis())!=1) return; // atomic double-send gate
        if(parts.isEmpty()) parts=manager.divideMessage(text);
        ArrayList<PendingIntent> sent=new ArrayList<>();
        for(int i=0;i<parts.size();i++){
            Intent intent=new Intent(c,DailySummarySentReceiver.class).setAction(DailySummarySentReceiver.ACTION)
                    .putExtra("report_id",id).putExtra("parts",parts.size());
            sent.add(PendingIntent.getBroadcast(c,(int)(id*31+i),intent,
                    PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        }
        try { manager.sendMultipartTextMessage(p.recipient(),null,parts,sent,null); }
        catch(RuntimeException error){ dao.finish(id,"ECHEC",error.getClass().getSimpleName(),System.currentTimeMillis()); }
    }
    @SuppressWarnings("deprecation") private static SmsManager selectManager(Context c){
        int id=SubscriptionManager.getDefaultSmsSubscriptionId();
        if(id==SubscriptionManager.INVALID_SUBSCRIPTION_ID) return null;
        if(android.os.Build.VERSION.SDK_INT>=31) return c.getSystemService(SmsManager.class).createForSubscriptionId(id);
        return SmsManager.getSmsManagerForSubscriptionId(id);
    }
}
