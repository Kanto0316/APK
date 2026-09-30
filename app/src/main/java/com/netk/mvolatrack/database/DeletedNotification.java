package com.netk.mvolatrack.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Stable tombstone preventing a deliberately deleted notification from being regenerated. */
@Entity(tableName = "deleted_notifications")
public final class DeletedNotification {
    @PrimaryKey @NonNull public final String transactionKey;

    public DeletedNotification(@NonNull String transactionKey) {
        this.transactionKey = transactionKey;
    }
}
