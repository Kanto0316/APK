package com.netk.mvolatrack.invoice;

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

import com.netk.mvolatrack.R;

/** Posts high-priority system notifications for successful invoice results. */
public final class InvoiceResultNotificationHelper {
    static final String CHANNEL_ID = "invoice_results_high";
    private static final String CHANNEL_NAME = "Résultats des factures";

    private InvoiceResultNotificationHelper() { }

    public static void show(Context context, String resultKey, String title, String message,
            String imageUri) {
        if (context == null || resultKey == null || title == null || message == null
                || imageUri == null) return;
        Context applicationContext = context.getApplicationContext();
        createChannelIfMissing(applicationContext);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(applicationContext,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        int notificationId = notificationId(resultKey);
        Intent intent = new Intent(applicationContext, InvoiceViewerActivity.class)
                .putExtra(InvoiceViewerActivity.EXTRA_IMAGE_URI, imageUri);
        PendingIntent openInvoice = PendingIntent.getActivity(applicationContext, notificationId,
                intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(
                applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_file_download_24)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(openInvoice)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManagerCompat.from(applicationContext).notify(notificationId,
                notification.build());
    }

    private static void createChannelIfMissing(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Téléchargements et tâches d’impression de factures terminés");
        manager.createNotificationChannel(channel);
    }

    static int notificationId(String resultKey) {
        return 0x50000000 ^ resultKey.hashCode();
    }
}
