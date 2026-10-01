package com.netk.mvolatrack.daily;

import android.Manifest;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.netk.mvolatrack.R;
import com.netk.mvolatrack.database.AppDatabase;
import com.netk.mvolatrack.database.DailySummaryReport;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public final class DailySummaryActivity extends AppCompatActivity {
    public static final String EXTRA_REPORT_ID="report_id";
    private SwitchMaterial enabled; private EditText recipient,template; private Button time;
    private TextView preview,previewState,segments,historyEmpty; private LinearLayout history;
    private int hour,minute; private DailySummaryPreferences preferences;
    private final ActivityResultLauncher<String> smsPermission=registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),granted->{if(!granted){enabled.setChecked(false);Toast.makeText(this,"Autorisation SMS refusée : l’envoi automatique reste désactivé.",Toast.LENGTH_LONG).show();}});
    @Override protected void onCreate(Bundle state){ super.onCreate(state); WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        setContentView(R.layout.activity_daily_summary); applyInsets(findViewById(R.id.dailyHeader));
        preferences=new DailySummaryPreferences(this); bind(); load(); refreshPreview(); observeHistory();
        long requested=getIntent().getLongExtra(EXTRA_REPORT_ID,-1);if(requested>0)showRequested(requested); }
    private void showRequested(long id){Executors.newSingleThreadExecutor().execute(()->{DailySummaryReport r=AppDatabase.getInstance(this).dailySummaryDao().get(id);if(r!=null)runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Résumé enregistré").setMessage("Destinataire : "+r.recipient+"\nÉtat : "+label(r.status)+"\n\n"+r.exactText).setPositiveButton(android.R.string.ok,null).show());});}
    private void bind(){ enabled=findViewById(R.id.dailyEnabled);recipient=findViewById(R.id.dailyRecipient);
        template=findViewById(R.id.dailyTemplate);time=findViewById(R.id.dailyTime);preview=findViewById(R.id.dailyPreview);
        previewState=findViewById(R.id.dailyPreviewState);segments=findViewById(R.id.dailySegments);
        history=findViewById(R.id.dailyHistory);historyEmpty=findViewById(R.id.dailyHistoryEmpty);
        findViewById(R.id.dailyBack).setOnClickListener(v->finish()); time.setOnClickListener(v->new TimePickerDialog(this,(p,h,m)->{hour=h;minute=m;showTime();},hour,minute,true).show());
        findViewById(R.id.dailySave).setOnClickListener(v->save());
        LinearLayout variables=findViewById(R.id.dailyVariables); for(String name:DailySummaryTemplate.VARIABLES){
            Button b=new Button(this);b.setText("{"+name+"}");b.setAllCaps(false);b.setOnClickListener(v->insert((String)b.getText()));variables.addView(b); }
        template.setOnFocusChangeListener((v,focused)->{if(!focused)refreshPreview();}); }
    private void load(){enabled.setChecked(preferences.enabled());recipient.setText(preferences.recipient());template.setText(preferences.template());hour=preferences.hour();minute=preferences.minute();showTime();}
    private void showTime(){time.setText(String.format(Locale.FRENCH,"Heure d’envoi : %02d:%02d",hour,minute));}
    private void insert(String value){int start=Math.max(0,template.getSelectionStart());template.getText().insert(start,value);refreshPreview();}
    private void save(){String model=template.getText().toString();String error=DailySummaryTemplate.validationError(model);
        String number=recipient.getText().toString().trim();if(error!=null){template.setError(error);return;}
        if(enabled.isChecked() && (!PhoneNumberUtils.isGlobalPhoneNumber(number)||number.replaceAll("\\D","").length()<8)){recipient.setError("Saisissez un numéro utilisable pour un SMS.");return;}
        if(enabled.isChecked() && androidx.core.content.ContextCompat.checkSelfPermission(this,Manifest.permission.SEND_SMS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){smsPermission.launch(Manifest.permission.SEND_SMS);return;}
        preferences.save(enabled.isChecked(),number,hour,minute,model);DailySummaryScheduler.reconcile(this);refreshPreview();Toast.makeText(this,"Configuration enregistrée.",Toast.LENGTH_SHORT).show();}
    private void refreshPreview(){final String model=template.getText()==null?preferences.template():template.getText().toString();Executors.newSingleThreadExecutor().execute(()->{
        long day=DailySummaryCalculator.previousDayStart(System.currentTimeMillis());DailySummary summary=DailySummaryCalculator.calculate(AppDatabase.getInstance(this).smsDao().getAllNewestFirst(),day);
        String error=DailySummaryTemplate.validationError(model);String text=error==null?DailySummaryTemplate.render(model,summary):error;
        int count=error==null?SmsManager.getDefault().divideMessage(text).size():0;runOnUiThread(()->{previewState.setText(summary.transactions==0?"Aucune transaction pour cette date":"Aperçu de la dernière journée terminée");preview.setText(text);segments.setText(error==null?count+" segment(s) SMS estimé(s) — accents et longueur peuvent augmenter ce nombre.":"");});});}
    private void observeHistory(){AppDatabase.getInstance(this).dailySummaryDao().observeAll().observe(this,this::showHistory);}
    private void showHistory(List<DailySummaryReport> reports){history.removeAllViews();historyEmpty.setVisibility(reports==null||reports.isEmpty()?View.VISIBLE:View.GONE);if(reports==null)return;
        SimpleDateFormat date=new SimpleDateFormat("dd/MM/yyyy",Locale.FRENCH);date.setTimeZone(DailySummaryCalculator.BUSINESS_ZONE);
        for(DailySummaryReport r:reports){TextView row=new TextView(this);row.setText(date.format(new Date(r.coveredDate))+"  •  "+r.recipient+"\n"+label(r.status)+(r.failureReason==null?"":" — "+r.failureReason));row.setTextSize(15);row.setPadding(16,18,16,18);row.setBackgroundResource(R.drawable.bg_history_card);row.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Résumé du "+date.format(new Date(r.coveredDate))).setMessage("Destinataire : "+r.recipient+"\nÉtat : "+label(r.status)+"\n\n"+r.exactText).setPositiveButton(android.R.string.ok,null).show());history.addView(row,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));}}
    private static String label(String status){if("EN_ATTENTE".equals(status))return "En attente";if("ENVOYE".equals(status))return "Envoyé avec succès";if("ECHEC".equals(status))return "Échec";if("HEURE_DEPASSEE".equals(status))return "Heure d’envoi dépassée";return "État incertain";}
    private static void applyInsets(View header){int h=header.getLayoutParams().height,l=header.getPaddingLeft(),t=header.getPaddingTop(),r=header.getPaddingRight(),b=header.getPaddingBottom();ViewCompat.setOnApplyWindowInsetsListener(header,(v,i)->{Insets top=i.getInsets(WindowInsetsCompat.Type.statusBars()|WindowInsetsCompat.Type.displayCutout());v.getLayoutParams().height=h+top.top;v.setPadding(l+top.left,t+top.top,r+top.right,b);return i;});}
}
