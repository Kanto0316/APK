package com.netk.mvolatrack.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.netk.mvolatrack.R;

/** Centralizes the completion notification for every file exported through SAF. */
public final class ExportNotificationHelper {
    private static final String CHANNEL_ID = "export_completed_high";
    private static final String CHANNEL_NAME = "Exports terminés";

    private ExportNotificationHelper() {}

    public static void showExportCompletedNotification(Context context, String fileName,
                                                        Uri uri, String mimeType) {
        Context applicationContext = context.getApplicationContext();
        createChannel(applicationContext);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(applicationContext,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent openIntent = new Intent(Intent.ACTION_VIEW);
        openIntent.setDataAndType(uri, mimeType);
        openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        openIntent.setClipData(ClipData.newRawUri(fileName, uri));
        Intent chooser = Intent.createChooser(openIntent, "Ouvrir l’export")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        int id = notificationId(uri);
        PendingIntent openFile = PendingIntent.getActivity(applicationContext, id, chooser,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(
                applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_file_download_24)
                .setContentTitle("Téléchargement terminé")
                .setContentText(fileName)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(fileName))
                .setContentIntent(openFile)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManagerCompat.from(applicationContext).notify(id, notification.build());
    }

    private static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Fichiers exportés par MVolaCash");
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private static int notificationId(Uri uri) {
        return 0x40000000 ^ uri.toString().hashCode();
    }
}
