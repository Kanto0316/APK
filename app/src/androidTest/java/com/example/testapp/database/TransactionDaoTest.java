package com.example.testapp.database;

import android.content.Context;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TransactionDaoTest {
    @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    private AppDatabase database;
    @Before public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).allowMainThreadQueries().build();
    }
    @After public void tearDown() { database.close(); }
    @Test public void insertsAndReadsTransaction() throws InterruptedException {
        database.transactionDao().insert(new Transaction(10L, "TELCO", "RECHARGE", 5000,
                "Ar", null, null, "Recharge 5000 Ar"));
        List<Transaction> saved = await(database.transactionDao().getRecentTransactions());
        assertNotNull(saved);
        assertEquals(1, saved.size());
        assertEquals(5000, saved.get(0).montant, 0);
    }
    @Test public void ignoresDuplicateSmsAndSortsNewestFirst() throws InterruptedException {
        SmsMessage oldMessage = SmsMessage.create("TELCO", "ancien", 10L, true);
        SmsMessage newMessage = SmsMessage.create("BANK", "nouveau", 20L, false);
        database.smsDao().insert(oldMessage);
        database.smsDao().insert(oldMessage);
        database.smsDao().insert(newMessage);

        List<SmsMessage> saved = await(database.smsDao().observeAllNewestFirst());
        assertNotNull(saved);
        assertEquals(2, saved.size());
        assertEquals("nouveau", saved.get(0).messageBody);
        assertEquals("ancien", saved.get(1).messageBody);
    }
    private static <T> T await(LiveData<T> liveData) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Object[] value = new Object[1];
        Observer<T> observer = item -> { value[0] = item; latch.countDown(); };
        liveData.observeForever(observer);
        latch.await(2, TimeUnit.SECONDS);
        liveData.removeObserver(observer);
        return (T) value[0];
    }
}
