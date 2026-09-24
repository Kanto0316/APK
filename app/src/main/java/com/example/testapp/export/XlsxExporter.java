package com.example.testapp.export;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Minimal OOXML writer: produces a real, interoperable XLSX without a heavyweight dependency. */
public final class XlsxExporter {
    private static final String[] HEADERS = {"N°", "Date et heure", "Type", "Numéro", "Nom",
            "Montant", "Réf", "Bonus", "Frais", "Solde"};
    private XlsxExporter() {}

    public static void write(OutputStream output, List<ExportTransaction> rows, String period,
                             String type, long exportedAt) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            entry(zip, "[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                    + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                    + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                    + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                    + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/></Types>");
            entry(zip, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            entry(zip, "xl/workbook.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Transactions\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            entry(zip, "xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>");
            entry(zip, "xl/styles.xml", styles());
            entry(zip, "xl/worksheets/sheet1.xml", sheet(rows, period, type, exportedAt));
        }
    }

    private static String styles() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"dd/mm/yyyy hh:mm\"/></numFmts>"
                + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>"
                + "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills>"
                + "<borders count=\"1\"><border/></borders><cellStyleXfs count=\"1\"><xf/></cellStyleXfs>"
                + "<cellXfs count=\"3\"><xf xfId=\"0\"/><xf xfId=\"0\" fontId=\"1\" applyFont=\"1\"/><xf xfId=\"0\" numFmtId=\"164\" applyNumberFormat=\"1\"/></cellXfs></styleSheet>";
    }

    private static String sheet(List<ExportTransaction> rows, String period, String type,
                                long exportedAt) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><cols>");
        double[] widths = {7, 20, 13, 17, 24, 15, 22, 15, 15, 15};
        for (int i = 0; i < widths.length; i++) xml.append("<col min=\"").append(i + 1)
                .append("\" max=\"").append(i + 1).append("\" width=\"").append(widths[i]).append("\" customWidth=\"1\"/>");
        xml.append("</cols><sheetData>");
        textRow(xml, 1, 1, "RÉCAPITULATIF DES TRANSACTIONS");
        textRow(xml, 2, 0, "Période : " + period);
        textRow(xml, 3, 0, "Type : " + type);
        textRow(xml, 4, 0, "Nombre de transactions : " + rows.size());
        textRow(xml, 5, 0, "Date d’export : " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH).format(new Date(exportedAt)));
        xml.append("<row r=\"7\">");
        for (int i = 0; i < HEADERS.length; i++) textCell(xml, cell(i, 7), HEADERS[i], 1);
        xml.append("</row>");
        int rowNumber = 8;
        for (ExportTransaction row : rows) {
            xml.append("<row r=\"").append(rowNumber).append("\">");
            numberCell(xml, cell(0, rowNumber), row.number, 0);
            numberCell(xml, cell(1, rowNumber), excelDate(row.dateTime), 2);
            textCell(xml, cell(2, rowNumber), row.type, 0);
            textCell(xml, cell(3, rowNumber), row.phone, 0);
            textCell(xml, cell(4, rowNumber), row.name, 0);
            numberCell(xml, cell(5, rowNumber), row.amount, 0);
            textCell(xml, cell(6, rowNumber), row.reference, 0);
            nullableNumber(xml, cell(7, rowNumber), row.bonus);
            nullableNumber(xml, cell(8, rowNumber), row.fee);
            nullableNumber(xml, cell(9, rowNumber), row.balance);
            xml.append("</row>"); rowNumber++;
        }
        return xml.append("</sheetData></worksheet>").toString();
    }

    private static double excelDate(long millis) { return millis / 86400000d + 25569d; }
    private static void nullableNumber(StringBuilder xml, String ref, Long value) {
        if (value != null) numberCell(xml, ref, value, 0);
    }
    private static void textRow(StringBuilder xml, int row, int style, String value) {
        xml.append("<row r=\"").append(row).append("\">"); textCell(xml, "A" + row, value, style); xml.append("</row>");
    }
    private static void textCell(StringBuilder xml, String ref, String value, int style) {
        xml.append("<c r=\"").append(ref).append("\" t=\"inlineStr\" s=\"").append(style)
                .append("\"><is><t xml:space=\"preserve\">").append(escape(value == null || value.trim().isEmpty() ? "-" : value)).append("</t></is></c>");
    }
    private static void numberCell(StringBuilder xml, String ref, double value, int style) {
        xml.append("<c r=\"").append(ref).append("\" s=\"").append(style).append("\"><v>").append(value).append("</v></c>");
    }
    private static String cell(int column, int row) { return String.valueOf((char) ('A' + column)) + row; }
    private static String escape(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    private static void entry(ZipOutputStream zip, String name, String value) throws IOException {
        zip.putNextEntry(new ZipEntry(name)); zip.write(value.getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
    }
}
