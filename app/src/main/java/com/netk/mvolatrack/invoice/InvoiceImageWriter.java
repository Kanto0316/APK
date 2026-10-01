package com.netk.mvolatrack.invoice;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.View;

import java.io.IOException;
import java.io.OutputStream;

public final class InvoiceImageWriter {
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

    public static void delete(Context context, Uri uri) {
        if (uri != null) context.getContentResolver().delete(uri, null, null);
    }
}
