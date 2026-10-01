package com.netk.mvolatrack.daily;

/** Immutable values used by both the preview and the background sender. */
public final class DailySummary {
    public final long dayStart;
    public int transactions, clients, deposits, withdrawals, credits;
    public long depositAmount, withdrawalAmount, creditAmount, bonus;
    public boolean bonusPartial;

    DailySummary(long dayStart) { this.dayStart = dayStart; }
}
