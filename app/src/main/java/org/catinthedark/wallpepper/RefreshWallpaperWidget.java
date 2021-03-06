package org.catinthedark.wallpepper;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Button;
import android.widget.RemoteViews;

import org.catinthedark.wallpepper.service.WallpaperService;


/**
 * Implementation of App Widget functionality.
 */
public class RefreshWallpaperWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        final int N = appWidgetIds.length;
        Log.d(MyActivity.TAG, "onUpdate");
        for (int i=0; i<N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.refresh_wallpaper_widget);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);

        Intent intent = new Intent(context, WallpaperService.class);
        intent.setAction(WallpaperService.ACTION_CHANGE_WALLPAPER);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        views.setOnClickPendingIntent(R.id.imageButton, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}


