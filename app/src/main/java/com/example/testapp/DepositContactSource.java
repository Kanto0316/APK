package com.example.testapp;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Reads ContactsContract away from the UI thread; callers cache the returned snapshot. */
final class DepositContactSource {
    interface Callback { void onLoaded(List<DepositRecipient> contacts); }

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    DepositContactSource(Context context) {
        this.context = context.getApplicationContext();
    }

    void load(Callback callback) {
        executor.execute(() -> {
            List<DepositRecipient> result = new ArrayList<>();
            String[] projection = {
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
            };
            try (Cursor cursor = context.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection, null, null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE NOCASE")) {
                if (cursor != null) {
                    int nameColumn = cursor.getColumnIndexOrThrow(projection[0]);
                    int numberColumn = cursor.getColumnIndexOrThrow(projection[1]);
                    while (cursor.moveToNext()) {
                        String number = DepositUssd.normalizeRecipientNumber(
                                cursor.getString(numberColumn));
                        if (number != null) {
                            result.add(new DepositRecipient(number, cursor.getString(nameColumn),
                                    0L, true));
                        }
                    }
                }
            } catch (SecurityException ignored) {
                // Permission may be revoked while the background query is running.
            }
            callback.onLoaded(result);
        });
    }
}
