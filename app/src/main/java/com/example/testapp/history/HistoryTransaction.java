package com.example.testapp.history;

/**
 * UI-facing shape reserved for a future history row.
 *
 * <p>This model deliberately has no persistence or SMS dependency. It documents the data the
 * history screen will eventually render without connecting that screen to existing transactions.</p>
 */
public final class HistoryTransaction {
    public final String date;
    public final String time;
    public final String type;
    public final String recipientNumber;
    public final long amount;
    public final Long fees;
    public final long totalAmount;
    public final String reference;
    public final String status;

    public HistoryTransaction(String date, String time, String type, String recipientNumber,
            long amount, Long fees, long totalAmount, String reference, String status) {
        this.date = date;
        this.time = time;
        this.type = type;
        this.recipientNumber = recipientNumber;
        this.amount = amount;
        this.fees = fees;
        this.totalAmount = totalAmount;
        this.reference = reference;
        this.status = status;
    }
}
