package com.netk.mvolatrack.export;

/** Immutable, parsed transaction data consumed by both document exporters. */
public final class ExportTransaction {
    private static final String MISSING = "-";
    public final long dateTime;
    public final String type;
    public final String phone;
    public final String name;
    public final long amount;
    public final String reference;
    public final Long bonus;
    public final Long fee;
    public final Long balance;
    public final String verificationStatus;

    public ExportTransaction(long dateTime, String type, String phone, String name,
                             long amount, String reference, Long bonus, Long fee, Long balance) {
        this(dateTime, type, phone, name, amount, reference, bonus, fee, balance,
                "Non vérifiable");
    }

    public ExportTransaction(long dateTime, String type, String phone, String name,
                             long amount, String reference, Long bonus, Long fee, Long balance,
                             String verificationStatus) {
        this.dateTime = Math.max(0L, dateTime);
        this.type = safeText(type);
        this.phone = safeText(phone);
        this.name = safeText(name);
        this.amount = Math.max(0L, amount);
        this.reference = safeText(reference);
        this.bonus = bonus == null ? 0L : bonus;
        this.fee = fee == null ? 0L : fee;
        this.balance = balance == null ? 0L : balance;
        this.verificationStatus = safeText(verificationStatus);
    }

    private static String safeText(String value) {
        return value == null || value.trim().isEmpty() ? MISSING : value;
    }
}
