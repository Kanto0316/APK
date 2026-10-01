package com.netk.mvolatrack.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Immutable SMS snapshot and its send lifecycle. */
@Entity(tableName = "daily_summary_reports", indices = {
        @Index(value = {"coveredDate", "recipient"}, unique = true), @Index("scheduledAt")})
public class DailySummaryReport {
    @PrimaryKey(autoGenerate = true) public long id;
    public long coveredDate;
    @NonNull public String timezone;
    @NonNull public String recipient;
    @NonNull public String exactText;
    public long scheduledAt;
    public Long attemptedAt;
    public Long confirmedAt;
    @NonNull public String status;
    public String failureReason;
    public int segmentCount;
    public int successfulParts;

    public DailySummaryReport(long coveredDate, @NonNull String timezone,
            @NonNull String recipient, @NonNull String exactText, long scheduledAt,
            Long attemptedAt, Long confirmedAt, @NonNull String status, String failureReason,
            int segmentCount, int successfulParts) {
        this.coveredDate = coveredDate; this.timezone = timezone; this.recipient = recipient;
        this.exactText = exactText; this.scheduledAt = scheduledAt; this.attemptedAt = attemptedAt;
        this.confirmedAt = confirmedAt; this.status = status; this.failureReason = failureReason;
        this.segmentCount = segmentCount; this.successfulParts = successfulParts;
    }
}
