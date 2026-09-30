package com.netk.mvolatrack.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.List;

@Dao
public abstract class NotificationBonusDao {
    static final int MAX_NOTIFICATIONS = 50;

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract long insert(NotificationBonus notification);

    /** Keeps insertion and retention cleanup in one database transaction. */
    @Transaction
    public long insertAndTrim(NotificationBonus notification) {
        if (isDeleted(notification.transactionKey)) return -1L;
        long id = insert(notification);
        deleteOutsideNewest(MAX_NOTIFICATIONS);
        return id;
    }

    @Query("SELECT * FROM bonus_notifications ORDER BY date DESC, id DESC LIMIT 50")
    public abstract LiveData<List<NotificationBonus>> observeAll();

    @Query("SELECT COUNT(*) FROM bonus_notifications WHERE is_read = 0")
    public abstract LiveData<Integer> observeUnreadCount();

    @Query("UPDATE bonus_notifications SET is_read = 1 WHERE id = :id AND is_read = 0")
    public abstract int markRead(long id);

    @Query("UPDATE bonus_notifications SET is_read = 1 WHERE is_read = 0")
    public abstract int markAllRead();

    @Query("SELECT EXISTS(SELECT 1 FROM deleted_notifications WHERE transactionKey = :key)")
    abstract boolean isDeleted(String key);

    @Query("INSERT OR IGNORE INTO deleted_notifications (transactionKey) "
            + "SELECT transactionKey FROM bonus_notifications WHERE id IN (:ids)")
    abstract void rememberDeletedKeys(List<Long> ids);

    @Query("DELETE FROM bonus_notifications WHERE id IN (:ids)")
    abstract int deleteRowsByIds(List<Long> ids);

    /** Atomically records permanent deletion before removing the visible inbox rows. */
    @Transaction
    public int deleteByIds(List<Long> ids) {
        rememberDeletedKeys(ids);
        return deleteRowsByIds(ids);
    }

    @Query("DELETE FROM bonus_notifications WHERE id NOT IN "
            + "(SELECT id FROM bonus_notifications ORDER BY date DESC, id DESC LIMIT :limit)")
    abstract int deleteOutsideNewest(int limit);
}
