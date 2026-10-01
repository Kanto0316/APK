package com.netk.mvolatrack.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao public interface DailySummaryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) long insert(DailySummaryReport value);
    @Query("SELECT * FROM daily_summary_reports ORDER BY coveredDate DESC")
    LiveData<List<DailySummaryReport>> observeAll();
    @Query("SELECT * FROM daily_summary_reports WHERE id=:id") DailySummaryReport get(long id);
    @Query("SELECT * FROM daily_summary_reports WHERE coveredDate=:date AND recipient=:recipient LIMIT 1")
    DailySummaryReport find(long date, String recipient);
    @Query("UPDATE daily_summary_reports SET attemptedAt=:at, status='INCERTAIN', failureReason=NULL WHERE id=:id AND status='EN_ATTENTE'")
    int beginAttempt(long id, long at);
    @Query("UPDATE daily_summary_reports SET status=:status, failureReason=:reason, confirmedAt=:confirmed WHERE id=:id")
    void finish(long id, String status, String reason, Long confirmed);
    @Query("UPDATE daily_summary_reports SET successfulParts=successfulParts+1 WHERE id=:id")
    void partSucceeded(long id);
}
