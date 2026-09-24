package com.example.testapp.export;

import static org.junit.Assert.assertTrue;

import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class PdfExporterTest {
    @Test
    public void hundredsOfTransactionsProduceAReadableMultipagePdf() throws Exception {
        List<ExportTransaction> rows = new ArrayList<>();
        for (int index = 0; index < 500; index++) {
            rows.add(new ExportTransaction(index + 1, 1790258400000L, "Retrait",
                    "0345079482", "Ravaka", 16000, "12345678901234567890",
                    null, index % 2 == 0 ? 0L : null, 4250L));
        }
        File file = new File(ApplicationProvider.getApplicationContext().getCacheDir(),
                "multipage.pdf");
        try (FileOutputStream output = new FileOutputStream(file)) {
            PdfExporter.write(output, rows, "Tous", "Tous", 1790260200000L);
        }

        try (ParcelFileDescriptor descriptor = ParcelFileDescriptor.open(file,
                ParcelFileDescriptor.MODE_READ_ONLY);
             PdfRenderer renderer = new PdfRenderer(descriptor)) {
            assertTrue(renderer.getPageCount() > 1);
        }
    }
}
