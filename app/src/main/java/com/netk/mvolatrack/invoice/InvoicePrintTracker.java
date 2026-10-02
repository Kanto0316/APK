package com.netk.mvolatrack.invoice;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintAttributes;
import android.print.PrintJob;
import android.print.PrintJobInfo;
import android.print.PrintManager;

import com.netk.mvolatrack.database.NotificationType;
import com.netk.mvolatrack.repository.BonusNotificationRepository;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Observes app-owned Android print jobs until a terminal, certain state is known. */
public final class InvoicePrintTracker implements Application.ActivityLifecycleCallbacks {
    private static final String PREFS = "invoice_print_jobs";
    private static final long POLL_INTERVAL_MS = 1_000L;
    private static final long TRACKING_TIMEOUT_MS = 10 * 60_000L;
    private static volatile InvoicePrintTracker instance;

    private final Context context;
    private final PrintManager printManager;
    private final SharedPreferences pending;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, PrintJob> liveJobs = new HashMap<>();
    private final Runnable poll = this::pollPendingJobs;
    private int startedActivities;

    private InvoicePrintTracker(Application application) {
        context = application.getApplicationContext();
        printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);
        pending = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        application.registerActivityLifecycleCallbacks(this);
    }

    public static void install(Application application) {
        get(application);
    }

    private static InvoicePrintTracker get(Context context) {
        if (instance == null) {
            synchronized (InvoicePrintTracker.class) {
                if (instance == null) {
                    instance = new InvoicePrintTracker((Application) context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public static PrintJob print(Activity activity, Uri uri, long invoiceNumber,
            String reference, long transactionAt) {
        PrintManager manager = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
        if (manager == null) throw new IllegalStateException("Service d’impression indisponible");
        String formatted = InvoiceNumberStore.format(invoiceNumber);
        String attempt = UUID.randomUUID().toString();
        String label = "Facture MVolaCash n°" + formatted + " [" + attempt + "]";
        PrintAttributes attributes = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        PrintJob job = manager.print(label,
                new InvoicePrintDocumentAdapter(activity, uri), attributes);
        get(activity).remember(attempt, label, job, formatted, reference, transactionAt, uri);
        return job;
    }

    private synchronized void remember(String attempt, String label, PrintJob job, String number,
            String reference, long transactionAt, Uri uri) {
        if (job == null) return;
        JSONObject value = new JSONObject();
        try {
            value.put("label", label);
            value.put("number", number);
            value.put("reference", reference == null || reference.trim().isEmpty()
                    ? "-" : reference.trim());
            value.put("transactionAt", transactionAt);
            value.put("createdAt", System.currentTimeMillis());
            value.put("uri", uri.toString());
            pending.edit().putString(attempt, value.toString()).apply();
            liveJobs.put(attempt, job);
            schedulePoll();
        } catch (JSONException ignored) {
            // Primitive values cannot normally fail; without metadata no success is announced.
        }
    }

    private void pollPendingJobs() {
        synchronized (this) {
            handler.removeCallbacks(poll);
            if (startedActivities == 0 || printManager == null) return;
            long now = System.currentTimeMillis();
            Map<String, ?> entries = pending.getAll();
            for (Map.Entry<String, ?> entry : entries.entrySet()) {
                if (!(entry.getValue() instanceof String)) continue;
                String attempt = entry.getKey();
                String encoded = (String) entry.getValue();
                try {
                    JSONObject value = new JSONObject(encoded);
                    if (now - value.getLong("createdAt") > TRACKING_TIMEOUT_MS) {
                        forget(attempt);
                        continue;
                    }
                    PrintJob job = liveJobs.get(attempt);
                    if (job == null) job = findByLabel(value.getString("label"));
                    if (job != null) {
                        liveJobs.put(attempt, job);
                        evaluate(attempt, encoded, job);
                    }
                } catch (JSONException ignored) {
                    forget(attempt);
                }
            }
            if (!pending.getAll().isEmpty()) handler.postDelayed(poll, POLL_INTERVAL_MS);
        }
    }

    private PrintJob findByLabel(String label) {
        for (PrintJob candidate : printManager.getPrintJobs()) {
            PrintJobInfo info = candidate.getInfo();
            if (info != null && info.getLabel() != null
                    && label.contentEquals(info.getLabel())) return candidate;
        }
        return null;
    }

    private void evaluate(String attempt, String encoded, PrintJob job) {
        PrintJobInfo info = job.getInfo();
        if (info == null) return;
        int state = info.getState();
        if (state == PrintJobInfo.STATE_COMPLETED) {
            // Remove synchronously first so recreation and later polls cannot duplicate the event.
            pending.edit().remove(attempt).commit();
            liveJobs.remove(attempt);
            createCompletedNotification(attempt, encoded);
        } else if (state == PrintJobInfo.STATE_CANCELED || state == PrintJobInfo.STATE_FAILED) {
            forget(attempt);
        }
    }

    private void forget(String attempt) {
        liveJobs.remove(attempt);
        pending.edit().remove(attempt).apply();
    }

    private void schedulePoll() {
        if (startedActivities > 0 && printManager != null) {
            handler.removeCallbacks(poll);
            handler.post(poll);
        }
    }

    private void createCompletedNotification(String attempt, String encoded) {
        try {
            JSONObject value = new JSONObject(encoded);
            String number = value.getString("number");
            String reference = value.getString("reference");
            long transactionAt = value.getLong("transactionAt");
            String date = new java.text.SimpleDateFormat("dd/MM/yyyy · HH:mm", java.util.Locale.FRENCH)
                    .format(new java.util.Date(transactionAt));
            new BonusNotificationRepository(context).addNotification(NotificationType.FACTURE,
                    "invoice-print-" + attempt, "Facture traitée par l’impression",
                    "Facture n°" + number + " — Réf. " + reference + " — " + date,
                    value.getString("uri"));
        } catch (JSONException ignored) {
            // Corrupt persisted state is not proof of a successful invoice that can be reopened.
        }
    }

    @Override public synchronized void onActivityStarted(Activity activity) {
        startedActivities++;
        schedulePoll();
    }

    @Override public synchronized void onActivityStopped(Activity activity) {
        if (startedActivities > 0) startedActivities--;
        if (startedActivities == 0) handler.removeCallbacks(poll);
    }

    @Override public void onActivityCreated(Activity activity, Bundle state) { }
    @Override public void onActivityResumed(Activity activity) { }
    @Override public void onActivityPaused(Activity activity) { }
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
    @Override public void onActivityDestroyed(Activity activity) { }
}
