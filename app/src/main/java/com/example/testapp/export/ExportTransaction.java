package com.example.testapp.export;

/** Immutable, parsed transaction data consumed by both document exporters. */
public final class ExportTransaction {
    public final int number;
    public final long dateTime;
    public final String type;
    public final String phone;
    public final String name;
    public final long amount;
    public final String reference;
    public final Long bonus;
    public final Long fee;
    public final Long balance;

    public ExportTransaction(int number, long dateTime, String type, String phone, String name,
                             long amount, String reference, Long bonus, Long fee, Long balance) {
        this.number = number;
        this.dateTime = dateTime;
        this.type = type;
        this.phone = phone;
        this.name = name;
        this.amount = amount;
        this.reference = reference;
        this.bonus = bonus;
        this.fee = fee;
        this.balance = balance;
    }
}
