package com.netk.mvolatrack.invoice;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.pdf.PrintedPdfDocument;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** One-page, fit-without-cropping adapter for the exact PNG invoice visual. */
final class InvoicePrintDocumentAdapter extends PrintDocumentAdapter {
    private final Context context;
    private final Uri invoiceUri;
    private PrintAttributes attributes;

    InvoicePrintDocumentAdapter(Context context, Uri invoiceUri) {
        this.context = context.getApplicationContext();
        this.invoiceUri = invoiceUri;
    }

    @Override public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
            CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {
        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }
        attributes = newAttributes;
        callback.onLayoutFinished(new PrintDocumentInfo.Builder("facture-mvolacash.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1).build(), !newAttributes.equals(oldAttributes));
    }

    @Override public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
            CancellationSignal cancellationSignal, WriteResultCallback callback) {
        if (!containsPage(pages, 0)) {
            callback.onWriteFinished(new PageRange[0]);
            return;
        }
        PrintedPdfDocument document = new PrintedPdfDocument(context, attributes);
        Bitmap source = null;
        Bitmap printImage = null;
        try (InputStream input = context.getContentResolver().openInputStream(invoiceUri);
             FileOutputStream output = new FileOutputStream(destination.getFileDescriptor())) {
            if (input == null) throw new IOException("Facture introuvable");
            source = BitmapFactory.decodeStream(input);
            if (source == null) throw new IOException("Image de facture illisible");
            printImage = InvoiceImageWriter.surroundForPrint(source);
            if (cancellationSignal.isCanceled()) {
                callback.onWriteCancelled();
                return;
            }
            android.graphics.pdf.PdfDocument.Page page = document.startPage(0);
            Canvas canvas = page.getCanvas();
            Rect content = page.getInfo().getContentRect();
            RectF destinationRect = fit(printImage.getWidth(), printImage.getHeight(), content);
            canvas.drawBitmap(printImage, null, destinationRect, null);
            document.finishPage(page);
            document.writeTo(output);
            callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
        } catch (IOException | RuntimeException error) {
            callback.onWriteFailed(error.getMessage());
        } finally {
            document.close();
            if (printImage != null) printImage.recycle();
            if (source != null) source.recycle();
        }
    }

    private static boolean containsPage(PageRange[] ranges, int page) {
        if (ranges == null) return false;
        for (PageRange range : ranges)
            if (page >= range.getStart() && page <= range.getEnd()) return true;
        return false;
    }

    static RectF fit(int imageWidth, int imageHeight, Rect bounds) {
        float scale = Math.min(bounds.width() / (float) imageWidth,
                bounds.height() / (float) imageHeight);
        float width = imageWidth * scale;
        float height = imageHeight * scale;
        float left = bounds.left + (bounds.width() - width) / 2f;
        float top = bounds.top + (bounds.height() - height) / 2f;
        return new RectF(left, top, left + width, top + height);
    }
}
