package com.netk.mvolatrack.invoice;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.netk.mvolatrack.CashPointStore;
import com.netk.mvolatrack.R;
import com.netk.mvolatrack.database.NotificationType;
import com.netk.mvolatrack.repository.BonusNotificationRepository;
import com.netk.mvolatrack.sms.ClientNumberNormalizer;
import com.netk.mvolatrack.sms.MvolaMessageParser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Customer-only invoice modal. Internal balances, fees, bonuses and statuses never enter this view. */
public final class InvoiceDialog {
    private static final AtomicBoolean EXPORTING = new AtomicBoolean(false);
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor();

    public void show(Activity activity, MvolaMessageParser.ParsedTransaction transaction) {
        if (activity.isFinishing()) return;
        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_invoice, null, false);
        View ticket = content.findViewById(R.id.invoiceTicket);
        TextView brand = content.findViewById(R.id.invoiceBrand);
        brand.setText(CashPointStore.invoiceBrand(activity));
        TextView number = content.findViewById(R.id.invoiceNumber);
        TextView amount = content.findViewById(R.id.invoiceAmount);
        LinearLayout rows = content.findViewById(R.id.invoiceRows);
        Button close = content.findViewById(R.id.invoiceClose);
        Button print = content.findViewById(R.id.invoicePrint);
        Button download = content.findViewById(R.id.invoiceDownload);
        InvoiceNumberStore store = new InvoiceNumberStore(activity);
        SavedInvoice savedInvoice = new SavedInvoice();

        number.setText(InvoiceNumberStore.format(store.peek()));
        addRow(rows, "Référence", transaction.reference);
        addRow(rows, "Date et heure", new SimpleDateFormat("dd/MM/yyyy  ·  HH:mm", Locale.FRENCH)
                .format(new Date(transaction.transactionAt)));
        addRow(rows, "Type de transaction", transaction.type);
        addRow(rows, "Numéro client", ClientNumberNormalizer.format(transaction.clientNumber));
        if ("Dépôt".equalsIgnoreCase(transaction.type)
                || "Retrait".equalsIgnoreCase(transaction.type))
            addRow(rows, "Nom d’utilisateur", presentName(transaction.clientName));
        amount.setText(formatAmount(transaction.amount));

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(content);
        dialog.setCanceledOnTouchOutside(false);
        close.setOnClickListener(v -> dialog.dismiss());
        print.setOnClickListener(v -> saveOrReuse(activity, transaction, ticket, number, store,
                savedInvoice, download, print, close, false));
        download.setOnClickListener(v -> saveOrReuse(activity, transaction, ticket, number, store,
                savedInvoice, download, print, close, true));
        dialog.setOnDismissListener(ignored -> {
            close.setEnabled(true);
            print.setEnabled(true);
            download.setEnabled(true);
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.copyFrom(window.getAttributes());
            params.width = Math.min((int) (activity.getResources().getDisplayMetrics().widthPixels * .94f),
                    (int) (620 * activity.getResources().getDisplayMetrics().density));
            params.height = (int) (activity.getResources().getDisplayMetrics().heightPixels * .92f);
            params.dimAmount = .62f;
            window.setAttributes(params);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
    }

    private static void saveOrReuse(Activity activity,
            MvolaMessageParser.ParsedTransaction transaction, View ticket, TextView number,
            InvoiceNumberStore store, SavedInvoice state, Button download, Button print,
            Button close, boolean isDownload) {
        if (state.uri != null) {
            if (isDownload) announceDownload(activity, transaction, state);
            else print(activity, transaction, state);
            return;
        }
        if (!EXPORTING.compareAndSet(false, true)) return;
        download.setEnabled(false);
        print.setEnabled(false);
        close.setEnabled(false);
        long assigned = store.peek();
        number.setText(InvoiceNumberStore.format(assigned));
        Bitmap bitmap;
        try {
            bitmap = InvoiceImageWriter.render(ticket);
        } catch (RuntimeException error) {
            finish(activity, download, print, close,
                    "Impossible de générer l’image de la facture.");
            return;
        }
        String fileName = "MVolaCash_Facture_" + InvoiceNumberStore.format(assigned) + "_"
                + InvoiceFileNames.sanitizeReference(transaction.reference) + ".png";
        WRITER.execute(() -> {
            Uri saved = null;
            String errorMessage = null;
            try {
                saved = InvoiceImageWriter.save(activity.getApplicationContext(), bitmap, fileName);
                InvoiceImageWriter.requireReadable(activity.getApplicationContext(), saved);
                if (!store.consume(assigned)) {
                    InvoiceImageWriter.delete(activity.getApplicationContext(), saved);
                    saved = null;
                    throw new java.io.IOException("Le numéro de facture n’a pas pu être sécurisé");
                }
                state.uri = saved;
                state.number = assigned;
            } catch (Exception error) {
                if (saved != null) InvoiceImageWriter.delete(activity.getApplicationContext(), saved);
                errorMessage = "Enregistrement impossible (stockage indisponible ou espace insuffisant). Aucun numéro n’a été consommé.";
            } finally {
                bitmap.recycle();
            }
            Uri result = saved;
            String failure = errorMessage;
            activity.runOnUiThread(() -> {
                finish(activity, download, print, close, failure);
                if (result != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    if (isDownload) announceDownload(activity, transaction, state);
                    else print(activity, transaction, state);
                }
            });
        });
    }

    private static void announceDownload(Activity activity,
            MvolaMessageParser.ParsedTransaction transaction, SavedInvoice state) {
        if (state.notificationCreated || state.uri == null) return;
        state.notificationCreated = true;
        String formatted = InvoiceNumberStore.format(state.number);
        String reference = transaction.reference == null || transaction.reference.trim().isEmpty()
                ? "-" : transaction.reference.trim();
        String resultKey = "invoice-download-" + state.uri;
        String message = "Facture n°" + formatted + " — Réf. " + reference;
        new BonusNotificationRepository(activity.getApplicationContext()).addNotification(
                NotificationType.FACTURE, resultKey, "Facture téléchargée", message,
                state.uri.toString());
        InvoiceResultNotificationHelper.show(activity, resultKey, "Facture téléchargée", message,
                state.uri.toString());
        Toast.makeText(activity, "Facture n°" + formatted + " enregistrée dans l’appareil",
                Toast.LENGTH_SHORT).show();
    }

    private static void print(Activity activity, MvolaMessageParser.ParsedTransaction transaction,
            SavedInvoice state) {
        if (state.uri == null) return;
        try {
            InvoicePrintTracker.print(activity, state.uri, state.number, transaction.reference,
                    transaction.transactionAt);
        } catch (RuntimeException error) {
            Toast.makeText(activity, "Impossible d’ouvrir l’impression sur cet appareil.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private static String presentName(String value) {
        return value == null || value.trim().isEmpty() || "-".equals(value.trim())
                ? "Non renseigné" : value.trim();
    }

    private static final class SavedInvoice {
        volatile Uri uri;
        volatile long number;
        boolean notificationCreated;
    }

    private static void finish(Activity activity, Button download, Button print, Button close,
                               String errorMessage) {
        EXPORTING.set(false);
        if (!activity.isFinishing() && !activity.isDestroyed()) {
            download.setEnabled(true);
            print.setEnabled(true);
            close.setEnabled(true);
            if (errorMessage != null)
                Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private static void addRow(LinearLayout parent, String label, String value) {
        if (value == null || value.trim().isEmpty() || "-".equals(value.trim())) return;
        android.content.Context context = parent.getContext();
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int p = Math.round(15 * context.getResources().getDisplayMetrics().density);
        row.setPadding(0, p, 0, p);
        TextView left = text(context, label, false);
        TextView right = text(context, value.trim(), true);
        row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1.25f);
        rightParams.setMarginStart(p / 2);
        row.addView(right, rightParams);
        parent.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        View divider = new View(context);
        divider.setBackgroundResource(R.color.sms_divider);
        parent.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                Math.max(1, Math.round(context.getResources().getDisplayMetrics().density))));
    }

    private static TextView text(android.content.Context context, String value, boolean bold) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextColor(context.getColor(bold ? R.color.sms_text_primary : R.color.sms_text_secondary));
        text.setTextSize(15);
        text.setGravity(bold ? android.view.Gravity.END : android.view.Gravity.START);
        if (bold) text.setTypeface(text.getTypeface(), android.graphics.Typeface.BOLD);
        return text;
    }

    private static String formatAmount(long amount) {
        return String.format(Locale.FRENCH, "%,d Ar", amount).replace('\u00a0', ' ')
                .replace('\u202f', ' ');
    }
}
