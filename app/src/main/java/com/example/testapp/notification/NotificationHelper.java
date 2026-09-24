package com.example.testapp.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.testapp.MainActivity;
import com.example.testapp.overlay.TransactionDisplayFormatter;
import com.example.testapp.sms.MvolaMessageParser;

/** Posts one independently actionable, normally audible notification per saved transaction. */
public final class NotificationHelper {
    public static final String EXTRA_TRANSACTION_ID = "transaction_id";
    private static final String CHANNEL_ID = "transactions_v2";
    private final Context context;

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        createChannel();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Transactions",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Transactions MVola reçues");
        channel.setSound(sound, null);
        channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PRIVATE);
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    public void showTransaction(long transactionId, String sender,
                                MvolaMessageParser.ParsedTransaction transaction) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;

        Intent intent = new Intent(context, MainActivity.class)
                .putExtra(EXTRA_TRANSACTION_ID, transactionId)
                .setData(Uri.parse("suivisms://transaction/" + transactionId))
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int requestCode = notificationId(transactionId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String title = sender == null || sender.trim().isEmpty()
                ? "Expéditeur inconnu" : sender.trim();
        String text = TransactionDisplayFormatter.text(transaction.type) + " • "
                + TransactionDisplayFormatter.amount(transaction.amount) + " • "
                + TransactionDisplayFormatter.phone(transaction.clientNumber);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        NotificationManagerCompat.from(context).notify(requestCode, builder.build());
    }

    /** Stable and distinct for practical Room IDs; PendingIntent also has a unique data URI. */
    static int notificationId(long transactionId) {
        return (int) (transactionId ^ (transactionId >>> 32));
    }
}
