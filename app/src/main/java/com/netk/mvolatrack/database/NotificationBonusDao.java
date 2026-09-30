package com.netk.mvolatrack.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface NotificationBonusDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) long insert(NotificationBonus notification);

    @Query("SELECT * FROM bonus_notifications ORDER BY transactionDate DESC, id DESC")
    LiveData<List<NotificationBonus>> observeAll();

    @Query("SELECT COUNT(*) FROM bonus_notifications WHERE isRead = 0")
    LiveData<Integer> observeUnreadCount();

    @Query("UPDATE bonus_notifications SET isRead = 1 WHERE isRead = 0")
    int markAllRead();
}
