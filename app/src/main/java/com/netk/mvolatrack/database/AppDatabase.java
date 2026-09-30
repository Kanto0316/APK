package com.netk.mvolatrack.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Transaction.class, SmsMessage.class, NotificationBonus.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract TransactionDao transactionDao();
    public abstract SmsDao smsDao();
    public abstract NotificationBonusDao notificationBonusDao();

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `sms_messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sender` TEXT NOT NULL, `messageBody` TEXT NOT NULL, `receivedDate` INTEGER NOT NULL, `readStatus` INTEGER NOT NULL, `uniqueKey` TEXT NOT NULL)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sms_messages_uniqueKey` ON `sms_messages` (`uniqueKey`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_sms_messages_receivedDate` ON `sms_messages` (`receivedDate`)");
        }
    };

    /** Adds the persistent bonus-notification inbox to databases created by version 2. */
    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `bonus_notifications` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `createdAt` INTEGER NOT NULL, `transactionKey` TEXT NOT NULL, `transactionReference` TEXT, `clientNumber` TEXT, `anomalyType` TEXT NOT NULL, `expectedBonus` INTEGER NOT NULL, `detectedBonus` INTEGER, `explanation` TEXT NOT NULL, `transactionDate` INTEGER NOT NULL, `isRead` INTEGER NOT NULL)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_bonus_notifications_transactionKey` ON `bonus_notifications` (`transactionKey`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_bonus_notifications_isRead` ON `bonus_notifications` (`isRead`)");
        }
    };

    /** Renames the read-state column without losing existing inbox entries. */
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE `bonus_notifications_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `createdAt` INTEGER NOT NULL, `transactionKey` TEXT NOT NULL, `transactionReference` TEXT, `clientNumber` TEXT, `anomalyType` TEXT NOT NULL, `expectedBonus` INTEGER NOT NULL, `detectedBonus` INTEGER, `explanation` TEXT NOT NULL, `transactionDate` INTEGER NOT NULL, `is_read` INTEGER NOT NULL DEFAULT 0)");
            database.execSQL("INSERT INTO `bonus_notifications_new` (`id`, `createdAt`, `transactionKey`, `transactionReference`, `clientNumber`, `anomalyType`, `expectedBonus`, `detectedBonus`, `explanation`, `transactionDate`, `is_read`) SELECT `id`, `createdAt`, `transactionKey`, `transactionReference`, `clientNumber`, `anomalyType`, `expectedBonus`, `detectedBonus`, `explanation`, `transactionDate`, `isRead` FROM `bonus_notifications`");
            database.execSQL("DROP TABLE `bonus_notifications`");
            database.execSQL("ALTER TABLE `bonus_notifications_new` RENAME TO `bonus_notifications`");
            database.execSQL("CREATE UNIQUE INDEX `index_bonus_notifications_transactionKey` ON `bonus_notifications` (`transactionKey`)");
            database.execSQL("CREATE INDEX `index_bonus_notifications_is_read` ON `bonus_notifications` (`is_read`)");
            database.execSQL("DELETE FROM `bonus_notifications` WHERE `id` NOT IN (SELECT `id` FROM `bonus_notifications` ORDER BY `transactionDate` DESC, `id` DESC LIMIT 50)");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "sms-tracker.db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                            .build();
                }
            }
        }
        return instance;
    }
}
