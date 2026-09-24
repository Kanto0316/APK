package com.example.testapp.export;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import com.example.testapp.sms.ClientNumberNormalizer;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** A4 landscape report with deterministic pagination and a repeated table header. */
public final class PdfExporter {
    private static final int PAGE_WIDTH = 842;
    private static final int PAGE_HEIGHT = 595;
    private static final float LEFT = 22;
    private static final float TOP = 24;
    private static final float ROW_HEIGHT = 22;
    private static final float[] WIDTHS = {26, 76, 48, 78, 93, 68, 92, 62, 62, 67};
    private static final String[] HEADERS = {"N°", "Date/heure", "Type", "Numéro", "Nom",
            "Montant", "Réf", "Bonus", "Frais", "Solde"};

    private PdfExporter() {}

    public static void write(OutputStream output, List<ExportTransaction> rows, String period,
                             String type, long exportedAt) throws IOException {
        PdfDocument document = new PdfDocument();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int index = 0;
        int pageNumber = 1;
        try {
            while (index < rows.size()) {
                PdfDocument.Page page = document.startPage(new PdfDocument.PageInfo.Builder(
                        PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create());
                Canvas canvas = page.getCanvas();
                float y = drawPageHeader(canvas, paint, period, type, rows.size(), exportedAt,
                        pageNumber);
                drawTableHeader(canvas, paint, y);
                y += ROW_HEIGHT;
                while (index < rows.size() && y + ROW_HEIGHT <= PAGE_HEIGHT - 22) {
                    drawRow(canvas, paint, y, rows.get(index));
                    y += ROW_HEIGHT;
                    index++;
                }
                document.finishPage(page);
                pageNumber++;
            }
            document.writeTo(output);
        } finally {
            document.close();
        }
    }

    private static float drawPageHeader(Canvas canvas, Paint paint, String period, String type,
                                        int count, long exportedAt, int page) {
        paint.setColor(Color.rgb(18, 75, 116)); paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(18); canvas.drawText("SUIVI SMS", LEFT, TOP, paint);
        paint.setTextSize(13); canvas.drawText("Récapitulatif des transactions", LEFT, TOP + 21, paint);
        paint.setColor(Color.DKGRAY); paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setTextSize(9);
        canvas.drawText("Période : " + period + "    Type : " + type
                + "    Nombre de transactions : " + count, LEFT, TOP + 42, paint);
        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH)
                .format(new Date(exportedAt));
        canvas.drawText("Date d’export : " + date, LEFT, TOP + 56, paint);
        canvas.drawText("Page " + page, PAGE_WIDTH - 60, TOP, paint);
        return TOP + 68;
    }

    private static void drawTableHeader(Canvas canvas, Paint paint, float y) {
        paint.setColor(Color.rgb(18, 75, 116)); canvas.drawRect(LEFT, y, right(), y + ROW_HEIGHT, paint);
        paint.setColor(Color.WHITE); paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(7.5f);
        float x = LEFT;
        for (int i = 0; i < HEADERS.length; i++) {
            canvas.drawText(HEADERS[i], x + 3, y + 14, paint); x += WIDTHS[i];
        }
    }

    private static void drawRow(Canvas canvas, Paint paint, float y, ExportTransaction row) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.WHITE); canvas.drawRect(LEFT, y, right(), y + ROW_HEIGHT, paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(.5f); paint.setColor(Color.LTGRAY);
        canvas.drawRect(LEFT, y, right(), y + ROW_HEIGHT, paint); paint.setStyle(Paint.Style.FILL);
        String date = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.FRENCH).format(new Date(row.dateTime));
        String[] values = {String.valueOf(row.number), date, row.type,
                ClientNumberNormalizer.format(row.phone), missing(row.name), ariary(row.amount),
                missing(row.reference), nullableAriary(row.bonus, false), nullableAriary(row.fee, true),
                nullableAriary(row.balance, false)};
        paint.setColor(Color.BLACK); paint.setTypeface(android.graphics.Typeface.DEFAULT); paint.setTextSize(7.2f);
        float x = LEFT;
        for (int i = 0; i < values.length; i++) {
            canvas.save(); canvas.clipRect(x + 2, y, x + WIDTHS[i] - 2, y + ROW_HEIGHT);
            canvas.drawText(ellipsize(paint, values[i], WIDTHS[i] - 6), x + 3, y + 14, paint);
            canvas.restore(); x += WIDTHS[i];
        }
    }

    private static String ellipsize(Paint paint, String value, float width) {
        if (paint.measureText(value) <= width) return value;
        String suffix = "…"; int end = value.length();
        while (end > 0 && paint.measureText(value, 0, end) + paint.measureText(suffix) > width) end--;
        return value.substring(0, end) + suffix;
    }
    private static String missing(String value) { return value == null || value.trim().isEmpty() ? "-" : value; }
    private static String nullableAriary(Long value, boolean zeroMissing) { return value == null || (zeroMissing && value == 0) ? "-" : ariary(value); }
    private static String ariary(long value) { return String.format(Locale.FRENCH, "%,d Ar", value).replace('\u00a0', ' ').replace('\u202f', ' '); }
    private static float right() { float value = LEFT; for (float width : WIDTHS) value += width; return value; }
}
