package org.catinthedark.wallpepper;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

/**
 * Created by Leyfer Kirill (kolbasisha@gmail.com) on 06.06.15.
 */
public class WallpepperNotification {
    public static final int NOTIFICATION_ID = 26682;

    private final CharSequence title;
    private final Context context;

    private PendingIntent pendingIntent;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private Notification notification;

    public WallpepperNotification(Context context, CharSequence title) {

        this.title = title;
        this.context = context;

        notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(android.R.drawable.ic_menu_gallery)
                .setContentTitle("Setting background...")
                .setProgress(0, 0, true);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notification = notificationBuilder.build();

        if (Build.VERSION.SDK_INT <= 10) {
            pendingIntent = PendingIntent.getService(context, 0, new Intent(context, MyActivity.class), 0);
            notification.setLatestEventInfo(context, title, "setting background", pendingIntent);
        }
    }

    public Notification getNotification() {
        return notification;
    }

    public void publishProgress(String message) {
        publishProgress(message, true, 0);
    }

    public void publishProgress(String message, boolean indeterminate, int CurrentProgress) {
        notificationBuilder.setContentText(message);
        if (indeterminate) {
            notificationBuilder.setProgress(0, 0, true);
        } else {
            notificationBuilder.setProgress(100, CurrentProgress, false);
        }

        notification = notificationBuilder.build();

        if (Build.VERSION.SDK_INT <= 10) {
            notification.setLatestEventInfo(context, title, message, pendingIntent);
        }

        notificationManager.notify(NOTIFICATION_ID, notification);
    }
}
