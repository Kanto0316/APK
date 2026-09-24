package com.example.testapp.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.testapp.MainActivity;
import com.example.testapp.R;
import com.example.testapp.database.Transaction;
import com.example.testapp.overlay.TransactionDisplayFormatter;
import com.example.testapp.sms.MvolaMessageParser;

import java.util.Locale;

public class NotificationHelper {
    private static final String CHANNEL_ID = "sms_analysis";
    private final Context context;

    public NotificationHelper(Context context) {
        this.context = context;
        createChannel();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Analyse des SMS", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Transactions détectées dans les SMS");
            channel.setSound(null, null);
            channel.enableVibration(false);
            context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    public void showTransactionSaved(Transaction transaction) {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String content = String.format(Locale.getDefault(), "%s : %,.0f %s",
                transaction.type, transaction.montant, transaction.devise);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle("SMS analysé et enregistré")
                .setContentText(content)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        NotificationManagerCompat.from(context).notify((int) transaction.date, builder.build());
    }

    /** Fallback used only when a valid parsed SMS cannot be shown as a system overlay. */
    public void showParsedTransaction(String sender,
                                      MvolaMessageParser.ParsedTransaction transaction) {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String number = TransactionDisplayFormatter.phone(transaction.clientNumber);
        String amount = TransactionDisplayFormatter.amount(transaction.type, transaction.amount)
                .replace("+ ", "+");
        String balance = TransactionDisplayFormatter.balance(transaction.balance);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle(TransactionDisplayFormatter.senderTitle(sender))
                .setContentText(number + " • " + amount)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(number + " • " + amount + "\nSolde : " + balance))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat.from(context).notify(
                (int) (transaction.transactionAt ^ (transaction.transactionAt >>> 32)),
                builder.build());
    }
}
