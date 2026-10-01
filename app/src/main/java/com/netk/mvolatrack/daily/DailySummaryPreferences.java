package com.netk.mvolatrack.daily;

import android.content.Context;
import android.content.SharedPreferences;

public final class DailySummaryPreferences {
    private static final String FILE="daily_summary", ENABLED="enabled", RECIPIENT="recipient",
            HOUR="hour", MINUTE="minute", TEMPLATE="template";
    private final SharedPreferences values;
    public DailySummaryPreferences(Context context) { values=context.getSharedPreferences(FILE,0); }
    public boolean enabled(){ return values.getBoolean(ENABLED,false); }
    public String recipient(){ return values.getString(RECIPIENT,""); }
    public int hour(){ return values.getInt(HOUR,8); }
    public int minute(){ return values.getInt(MINUTE,0); }
    public String template(){ return values.getString(TEMPLATE,DailySummaryTemplate.DEFAULT); }
    public void save(boolean enabled,String recipient,int hour,int minute,String template){
        values.edit().putBoolean(ENABLED,enabled).putString(RECIPIENT,recipient)
                .putInt(HOUR,hour).putInt(MINUTE,minute).putString(TEMPLATE,template).apply();
    }
}
