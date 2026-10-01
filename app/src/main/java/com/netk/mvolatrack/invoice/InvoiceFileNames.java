package com.netk.mvolatrack.invoice;

public final class InvoiceFileNames {
    private InvoiceFileNames() {}

    public static String sanitizeReference(String reference) {
        String safe = reference == null ? "transaction" : reference.trim();
        safe = safe.replaceAll("[^A-Za-z0-9._-]+", "_")
                .replaceAll("^[._-]+|[._-]+$", "");
        return safe.isEmpty() ? "transaction" : safe;
    }
}
