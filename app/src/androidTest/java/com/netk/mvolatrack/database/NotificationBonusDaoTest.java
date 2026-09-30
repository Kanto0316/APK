package com.netk.mvolatrack.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class NotificationBonusDaoTest {
    @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    private AppDatabase database;
    private NotificationBonusDao dao;

    @Before public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries().build();
        dao = database.notificationBonusDao();
    }

    @After public void tearDown() {
        if (database != null) database.close();
    }

    @Test public void newNotificationIncrementsUnreadBadgeAndReadingKeepsHistory() throws Exception {
        long id = dao.insertAndTrim(notification(1));
        assertEquals(1, (int) await(dao.observeUnreadCount()));

        assertEquals(1, dao.markRead(id));
        assertEquals(0, (int) await(dao.observeUnreadCount()));
        List<NotificationBonus> history = await(dao.observeAll());
        assertEquals(1, history.size());
        assertTrue(history.get(0).isRead);
    }

    @Test public void markAllReadResetsUnreadBadge() throws Exception {
        dao.insertAndTrim(notification(1));
        dao.insertAndTrim(notification(2));
        assertEquals(2, (int) await(dao.observeUnreadCount()));

        assertEquals(2, dao.markAllRead());
        assertEquals(0, (int) await(dao.observeUnreadCount()));
    }

    @Test public void keepsOnlyTheFiftyNewestNotifications() throws Exception {
        for (int index = 1; index <= 55; index++) dao.insertAndTrim(notification(index));

        List<NotificationBonus> values = await(dao.observeAll());
        assertEquals(50, values.size());
        assertEquals("key-55", values.get(0).transactionKey);
        assertEquals("key-6", values.get(49).transactionKey);
        assertEquals(50, (int) await(dao.observeUnreadCount()));
    }

    @Test public void emptyInboxHasNoRowsAndNoUnreadBadge() throws Exception {
        assertTrue(await(dao.observeAll()).isEmpty());
        assertEquals(0, (int) await(dao.observeUnreadCount()));
    }

    @Test public void deleteByIdsRemovesOnlySelectionAndUpdatesUnreadBadge() throws Exception {
        long first = dao.insertAndTrim(notification(1));
        long second = dao.insertAndTrim(notification(2));
        long third = dao.insertAndTrim(notification(3));
        dao.markRead(second);

        assertEquals(2, dao.deleteByIds(Arrays.asList(first, second)));
        List<NotificationBonus> remaining = await(dao.observeAll());
        assertEquals(1, remaining.size());
        assertEquals(third, remaining.get(0).id);
        assertEquals(1, (int) await(dao.observeUnreadCount()));
    }

    @Test public void readStateSurvivesDatabaseRestart() throws Exception {
        database.close();
        database = null;
        Context context = ApplicationProvider.getApplicationContext();
        String name = "notification-restart-test.db";
        context.deleteDatabase(name);
        AppDatabase first = Room.databaseBuilder(context, AppDatabase.class, name)
                .allowMainThreadQueries().build();
        long id = first.notificationBonusDao().insertAndTrim(notification(1));
        first.notificationBonusDao().markRead(id);
        first.close();

        AppDatabase reopened = Room.databaseBuilder(context, AppDatabase.class, name)
                .allowMainThreadQueries().build();
        List<NotificationBonus> values = await(reopened.notificationBonusDao().observeAll());
        assertNotNull(values);
        assertEquals(1, values.size());
        assertTrue(values.get(0).isRead);
        reopened.close();
        context.deleteDatabase(name);
    }

    private static NotificationBonus notification(int index) {
        return new NotificationBonus(index, "key-" + index, "ref-" + index,
                "0340000000", "BONUS_PARTIEL", 100, 50L, "Détail", index, false);
    }

    private static <T> T await(LiveData<T> liveData) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Object[] value = new Object[1];
        Observer<T> observer = item -> { value[0] = item; latch.countDown(); };
        liveData.observeForever(observer);
        assertTrue("LiveData value timed out", latch.await(2, TimeUnit.SECONDS));
        liveData.removeObserver(observer);
        return (T) value[0];
    }
}
