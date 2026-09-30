package com.netk.mvolatrack.database;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/** Persisted inbox entry created from an existing balance-verification result. */
@Entity(tableName = "bonus_notifications", indices = {
        @Index(value = "transactionKey", unique = true), @Index("isRead")})
public class NotificationBonus {
    @PrimaryKey(autoGenerate = true) public long id;
    public long createdAt;
    @NonNull public String transactionKey;
    public String transactionReference;
    public String clientNumber;
    @NonNull public String anomalyType;
    public long expectedBonus;
    public Long detectedBonus;
    @NonNull public String explanation;
    public long transactionDate;
    public boolean isRead;

    public NotificationBonus(long createdAt, @NonNull String transactionKey, String transactionReference,
                             String clientNumber, @NonNull String anomalyType, long expectedBonus,
                             Long detectedBonus, @NonNull String explanation, long transactionDate,
                             boolean isRead) {
        this.createdAt = createdAt;
        this.transactionKey = transactionKey;
        this.transactionReference = transactionReference;
        this.clientNumber = clientNumber;
        this.anomalyType = anomalyType;
        this.expectedBonus = expectedBonus;
        this.detectedBonus = detectedBonus;
        this.explanation = explanation;
        this.transactionDate = transactionDate;
        this.isRead = isRead;
    }
}
