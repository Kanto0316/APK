package com.netk.mvolatrack.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class XlsxExporterTest {
    @Test
    public void createsRealXlsxWithTextIdentifiersAndNumericAmounts() throws Exception {
        List<ExportTransaction> rows = new ArrayList<>();
        rows.add(new ExportTransaction(1, 1790258400000L, "Retrait", "0345079482",
                "Ravaka", 16000, "12345678901234567890", null, 0L, 4250L));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        XlsxExporter.write(output, rows, "Aujourd’hui", "Retrait", 1790260200000L);

        assertEquals('P', output.toByteArray()[0]);
        assertEquals('K', output.toByteArray()[1]);
        String sheet = zipEntry(output.toByteArray(), "xl/worksheets/sheet1.xml");
        assertTrue(sheet.contains("RÉCAPITULATIF DES TRANSACTIONS"));
        assertTrue(sheet.contains("t=\"inlineStr\" s=\"0\"><is><t xml:space=\"preserve\">0345079482"));
        assertTrue(sheet.contains("12345678901234567890"));
        assertTrue(sheet.contains("<c r=\"F8\" s=\"0\"><v>16000.0</v></c>"));
        assertFalse(sheet.contains("message brut"));
    }

    @Test
    public void supportsHundredsOfRowsAndKeepsNullNumericCellsEmpty() throws Exception {
        List<ExportTransaction> rows = new ArrayList<>();
        for (int index = 0; index < 500; index++) {
            rows.add(new ExportTransaction(index + 1, index * 1000L, "Dépôt",
                    "0340000000", "Client", index, "REF" + index, null, null, null));
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        XlsxExporter.write(output, rows, "Tous", "Tous", 0);

        String sheet = zipEntry(output.toByteArray(), "xl/worksheets/sheet1.xml");
        assertTrue(sheet.contains("<row r=\"507\">"));
        assertFalse(sheet.contains("r=\"H8\""));
        assertFalse(sheet.contains("r=\"I8\""));
    }

    private static String zipEntry(byte[] bytes, String expected) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (expected.equals(entry.getName())) {
                    ByteArrayOutputStream content = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024]; int count;
                    while ((count = zip.read(buffer)) != -1) content.write(buffer, 0, count);
                    return new String(content.toByteArray(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new AssertionError("Entrée XLSX absente : " + expected);
    }
}
