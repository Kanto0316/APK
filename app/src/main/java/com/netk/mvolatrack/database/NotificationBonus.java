package com.netk.mvolatrack.database;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.annotation.NonNull;

/** Persisted application notification. Legacy bonus columns remain for rich bonus details. */
@Entity(tableName = "bonus_notifications", indices = {
        @Index(value = "transactionKey", unique = true), @Index("is_read")})
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
    @ColumnInfo(name = "is_read", defaultValue = "0")
    public boolean isRead;
    @NonNull public String type;
    @NonNull public String title;
    @NonNull public String message;
    public long date;
    public String metadata;

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
        this.type = NotificationType.BONUS.name();
        this.title = bonusTitle(anomalyType);
        this.message = explanation;
        this.date = transactionDate;
        this.metadata = null;
    }

    public static NotificationBonus general(long date, @NonNull String uniqueKey,
            @NonNull NotificationType type, @NonNull String title, @NonNull String message,
            String metadata) {
        NotificationBonus value = new NotificationBonus(date, uniqueKey, null, null,
                type.name(), 0, null, message, date, false);
        value.type = type.name();
        value.title = title;
        value.message = message;
        value.metadata = metadata;
        return value;
    }

    private static String bonusTitle(String anomalyType) {
        if ("BONUS_SUPERIEUR".equals(anomalyType)) return "Bonus supérieur";
        if ("BONUS_NON_CREDITE".equals(anomalyType)) return "Bonus non crédité";
        if ("BONUS_PARTIEL".equals(anomalyType)) return "Bonus partiel";
        if ("BONUS_VERIFIE".equals(anomalyType)) return "Bonus vérifié";
        return "Bonus non vérifiable";
    }
}
