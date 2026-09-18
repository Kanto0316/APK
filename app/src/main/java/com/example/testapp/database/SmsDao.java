package com.example.testapp.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SmsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(SmsMessage message);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    List<Long> insertAll(List<SmsMessage> messages);

    @Query("SELECT * FROM sms_messages ORDER BY receivedDate DESC, id DESC")
    List<SmsMessage> getAllNewestFirst();

    @Query("SELECT * FROM sms_messages ORDER BY receivedDate DESC, id DESC")
    LiveData<List<SmsMessage>> observeAllNewestFirst();

    // These queries keep future search/filter features behind the database abstraction.
    @Query("SELECT * FROM sms_messages WHERE sender = :sender ORDER BY receivedDate DESC, id DESC")
    LiveData<List<SmsMessage>> observeBySender(String sender);

    @Query("SELECT * FROM sms_messages WHERE sender LIKE '%' || :query || '%' "
            + "OR messageBody LIKE '%' || :query || '%' ORDER BY receivedDate DESC, id DESC")
    LiveData<List<SmsMessage>> search(String query);
}
