package com.example.testapp.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** A complete SMS stored privately in the application's Room database. */
@Entity(tableName = "sms_messages", indices = {
        @Index(value = "uniqueKey", unique = true),
        @Index(value = "receivedDate")
})
public class SmsMessage {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String sender;

    @NonNull
    public String messageBody;

    public long receivedDate;
    public boolean readStatus;

    /** SHA-256(sender + date + body), used to make receiver/import retries idempotent. */
    @NonNull
    public String uniqueKey;

    public SmsMessage(@NonNull String sender, @NonNull String messageBody, long receivedDate,
                      boolean readStatus, @NonNull String uniqueKey) {
        this.sender = sender;
        this.messageBody = messageBody;
        this.receivedDate = receivedDate;
        this.readStatus = readStatus;
        this.uniqueKey = uniqueKey;
    }

    public static SmsMessage create(String sender, String body, long date, boolean read) {
        String safeSender = sender == null || sender.trim().isEmpty() ? "Expéditeur inconnu" : sender;
        String safeBody = body == null ? "" : body;
        return new SmsMessage(safeSender, safeBody, date, read,
                fingerprint(safeSender, date, safeBody));
    }

    public static String fingerprint(String sender, long date, String body) {
        String value = sender + '\u0000' + date + '\u0000' + body;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) result.append(String.format("%02x", item & 0xff));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 indisponible", impossible);
        }
    }
}
