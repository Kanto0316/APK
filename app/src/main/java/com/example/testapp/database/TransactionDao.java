package com.example.testapp.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    long insert(Transaction transaction);

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC LIMIT 100")
    LiveData<List<Transaction>> getRecentTransactions();

    @Query("SELECT * FROM transactions WHERE solde IS NOT NULL ORDER BY date DESC, id DESC LIMIT 1")
    LiveData<Transaction> getLatestBalance();

    @Query("SELECT * FROM transactions WHERE bonus IS NOT NULL ORDER BY date DESC, id DESC LIMIT 1")
    LiveData<Transaction> getLatestBonus();
}
