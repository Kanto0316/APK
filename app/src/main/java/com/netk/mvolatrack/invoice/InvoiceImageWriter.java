package com.netk.mvolatrack.invoice;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.View;

import java.io.IOException;
import java.io.OutputStream;

public final class InvoiceImageWriter {
    private static final int PRINT_SIDE_MARGIN = 48;
    private static final int PRINT_VERTICAL_MARGIN = 96;
    private static final int PRINT_BACKGROUND = Color.rgb(246, 246, 246);
    private InvoiceImageWriter() {}

    /** Renders the receipt view alone at print-friendly resolution, never a screen capture. */
    public static Bitmap render(View invoice) {
        int width = 1440;
        int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        invoice.measure(widthSpec, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        invoice.layout(0, 0, width, invoice.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(width, invoice.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        invoice.draw(new Canvas(bitmap));
        return bitmap;
    }

    /**
     * Adds a subtle non-white surround for printing. The source keeps its transparent perforation
     * cut-outs, so the unchanged white ticket silhouette remains visible in an A4/PDF preview.
     */
    public static Bitmap surroundForPrint(Bitmap ticket) {
        if (ticket == null) throw new IllegalArgumentException("ticket == null");
        Bitmap result = Bitmap.createBitmap(ticket.getWidth() + PRINT_SIDE_MARGIN * 2,
                ticket.getHeight() + PRINT_VERTICAL_MARGIN * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawColor(PRINT_BACKGROUND);
        canvas.drawBitmap(ticket, PRINT_SIDE_MARGIN, PRINT_VERTICAL_MARGIN, null);
        return result;
    }

    public static Uri save(Context context, Bitmap bitmap, String displayName) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MVolaCash/Factures");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("Impossible de créer le fichier image");
        boolean complete = false;
        try (OutputStream output = resolver.openOutputStream(uri, "w")) {
            if (output == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                throw new IOException("Impossible d’écrire l’image PNG");
            output.flush();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues published = new ContentValues();
                published.put(MediaStore.Images.Media.IS_PENDING, 0);
                if (resolver.update(uri, published, null, null) != 1)
                    throw new IOException("Impossible de publier l’image");
            }
            complete = true;
        } finally {
            if (!complete) resolver.delete(uri, null, null);
        }
        return uri;
    }

    /** Confirms that the published MediaStore image can actually be reopened by the app. */
    public static void requireReadable(Context context, Uri uri) throws IOException {
        if (uri == null) throw new IOException("Image absente");
        try (android.os.ParcelFileDescriptor descriptor =
                     context.getContentResolver().openFileDescriptor(uri, "r")) {
            if (descriptor == null) throw new IOException("Image inaccessible");
        }
    }

    public static void delete(Context context, Uri uri) {
        if (uri != null) context.getContentResolver().delete(uri, null, null);
    }
}
