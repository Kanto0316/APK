package com.netk.mvolatrack.invoice;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.print.PrintAttributes;
import android.print.PrintJob;
import android.print.PrintJobId;
import android.print.PrintManager;

import com.netk.mvolatrack.database.NotificationType;
import com.netk.mvolatrack.repository.BonusNotificationRepository;

import org.json.JSONException;
import org.json.JSONObject;

/** Persists and observes app-owned Android print jobs until a terminal, certain state is known. */
public final class InvoicePrintTracker {
    private static final String PREFS = "invoice_print_jobs";
    private static volatile InvoicePrintTracker instance;

    private final Context context;
    private final PrintManager printManager;
    private final SharedPreferences pending;

    private InvoicePrintTracker(Context context) {
        this.context = context.getApplicationContext();
        printManager = (PrintManager) this.context.getSystemService(Context.PRINT_SERVICE);
        pending = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (printManager != null) {
            printManager.addPrintJobStateChangeListener(this::onPrintJobStateChanged);
            checkPendingJobs();
        }
    }

    public static void install(Application application) {
        get(application);
    }

    private static InvoicePrintTracker get(Context context) {
        if (instance == null) {
            synchronized (InvoicePrintTracker.class) {
                if (instance == null) instance = new InvoicePrintTracker(context);
            }
        }
        return instance;
    }

    public static PrintJob print(Activity activity, Uri uri, long invoiceNumber,
            String reference, long transactionAt) {
        PrintManager manager = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
        if (manager == null) throw new IllegalStateException("Service d’impression indisponible");
        String formatted = InvoiceNumberStore.format(invoiceNumber);
        PrintAttributes attributes = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        PrintJob job = manager.print("Facture MVolaCash n°" + formatted,
                new InvoicePrintDocumentAdapter(activity, uri), attributes);
        get(activity).remember(job, formatted, reference, transactionAt, uri);
        return job;
    }

    private void remember(PrintJob job, String number, String reference, long transactionAt,
            Uri uri) {
        if (job == null || job.getId() == null) return;
        JSONObject value = new JSONObject();
        try {
            value.put("number", number);
            value.put("reference", reference == null || reference.trim().isEmpty()
                    ? "-" : reference.trim());
            value.put("transactionAt", transactionAt);
            value.put("uri", uri.toString());
            pending.edit().putString(job.getId().flattenToString(), value.toString()).apply();
            evaluate(job);
        } catch (JSONException ignored) {
            // All values are primitive; this is defensive and deliberately creates no success event.
        }
    }

    private void onPrintJobStateChanged(PrintJobId id) {
        if (id == null || printManager == null) return;
        PrintJob job = printManager.getPrintJob(id);
        if (job != null) evaluate(job);
    }

    /** Also covers process/activity recreation and return from the system print UI. */
    private void checkPendingJobs() {
        for (PrintJob job : printManager.getPrintJobs()) evaluate(job);
    }

    private synchronized void evaluate(PrintJob job) {
        String id = job.getId().flattenToString();
        String encoded = pending.getString(id, null);
        if (encoded == null) return;
        if (job.isCompleted()) {
            // Remove first: repeated callbacks/restarts can never produce a duplicate.
            pending.edit().remove(id).commit();
            createCompletedNotification(id, encoded);
        } else if (job.isCancelled() || job.isFailed()) {
            pending.edit().remove(id).apply();
        }
        // Queued, started and blocked jobs remain pending and are never reported as successful.
    }

    private void createCompletedNotification(String printJobId, String encoded) {
        try {
            JSONObject value = new JSONObject(encoded);
            String number = value.getString("number");
            String reference = value.getString("reference");
            long transactionAt = value.getLong("transactionAt");
            String date = new java.text.SimpleDateFormat("dd/MM/yyyy · HH:mm", java.util.Locale.FRENCH)
                    .format(new java.util.Date(transactionAt));
            new BonusNotificationRepository(context).addNotification(NotificationType.FACTURE,
                    "invoice-print-" + printJobId, "Facture traitée par l’impression",
                    "Facture n°" + number + " — Réf. " + reference + " — " + date,
                    value.getString("uri"));
        } catch (JSONException ignored) {
            // Corrupt persisted state is not proof of a successful invoice that can be reopened.
        }
    }
}
