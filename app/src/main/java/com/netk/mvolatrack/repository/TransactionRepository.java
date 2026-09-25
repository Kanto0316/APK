package com.netk.mvolatrack.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.netk.mvolatrack.database.AppDatabase;
import com.netk.mvolatrack.database.Transaction;
import com.netk.mvolatrack.database.TransactionDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionRepository {
    private static final ExecutorService DATABASE_EXECUTOR = Executors.newSingleThreadExecutor();
    private final TransactionDao dao;

    public TransactionRepository(Context context) {
        dao = AppDatabase.getInstance(context).transactionDao();
    }

    public void insert(Transaction transaction, Runnable onComplete) {
        DATABASE_EXECUTOR.execute(() -> {
            dao.insert(transaction);
            if (onComplete != null) onComplete.run();
        });
    }

    public LiveData<List<Transaction>> getRecentTransactions() { return dao.getRecentTransactions(); }
    public LiveData<Transaction> getLatestBalance() { return dao.getLatestBalance(); }
    public LiveData<Transaction> getLatestBonus() { return dao.getLatestBonus(); }
}
