package org.catinthedark.wallpepper.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.gson.Gson;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.catinthedark.wallpepper.MyActivity;
import org.catinthedark.wallpepper.R;
import org.catinthedark.wallpepper.json.JsonHelpers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class WallpaperService extends IntentService {
    public static final String ACTION_CHANGE_WALLPAPER = "org.catinthedark.wallpepper.service.action.CHANGE_WALLPAPER";
    public static final String EXTRA_TAGS = "org.catinthedark.wallpepper.service.extra.TAGS";
    public static final String EXTRA_LOWRES = "org.catinthedark.wallpepper.service.extra.LOWRES";
    public static final String EXTRA_RANDOM_RANGE = "org.catinthedark.wallpepper.service.extra.RANDOM_RANGE";

    private final Uri.Builder getImageIdsUriBuilder = new Uri.Builder()
            .scheme("https")
            .authority("api.flickr.com")
            .appendPath("services")
            .appendPath("rest")
            .appendQueryParameter("method", "flickr.photos.search")
            .appendQueryParameter("tag_mode", "all")
            .appendQueryParameter("sort", "interestingness-desc")
            .appendQueryParameter("page", "1")
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1");

    private final Uri.Builder getImageSizesUriBuilder = new Uri.Builder()
            .scheme("https")
            .authority("api.flickr.com")
            .appendPath("services")
            .appendPath("rest")
            .appendQueryParameter("method", "flickr.photos.getSizes")
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1");

    private PendingIntent pendingIntent;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private final int NOTIFICATION_ID = 26682;
    private final int RANDOM_RANGE = 15;

    private static final String TAG = MyActivity.TAG;

    /**
     * Starts this service to find image on Flickr service with the given tags,
     * download this image and set this as wallpaper
     *
     * @see IntentService
     */
    public static void startChangeWallpaper(Context context, String tags, int randomRange, boolean lowRes) {
        Intent intent = new Intent(context, WallpaperService.class);
        intent.setAction(ACTION_CHANGE_WALLPAPER);
        intent.putExtra(EXTRA_TAGS, tags);
        intent.putExtra(EXTRA_RANDOM_RANGE, randomRange);
        intent.putExtra(EXTRA_LOWRES, lowRes);
        context.startService(intent);
    }

    public WallpaperService() {
        super("WallpaperService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Context context = getApplicationContext();

        notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(android.R.drawable.ic_menu_gallery)
                .setContentTitle("Setting background...")
                .setProgress(0, 0, true);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Notification notification = notificationBuilder.build();

        if (Build.VERSION.SDK_INT <= 10) {
            pendingIntent = PendingIntent.getService(getApplicationContext(), 0, new Intent(this, MyActivity.class), 0);
            notification.setLatestEventInfo(this, getText(R.string.app_name), "setting background", pendingIntent);
        }

        startForeground(NOTIFICATION_ID, notification);
        return START_NOT_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_CHANGE_WALLPAPER.equals(action)) {

                SharedPreferences preferences = getSharedPreferences(MyActivity.SHARED_PREFS_NAME, MODE_PRIVATE);

                String tags = "";
                if (intent.hasExtra(EXTRA_TAGS)) {
                    tags = intent.getStringExtra(EXTRA_TAGS);
                } else if (preferences.contains(MyActivity.TAGS_KEY)) {
                    tags = preferences.getString(MyActivity.TAGS_KEY, "");
                }

                int randomRange = RANDOM_RANGE;
                if (intent.hasExtra(EXTRA_RANDOM_RANGE)) {
                    randomRange = intent.getIntExtra(EXTRA_RANDOM_RANGE, RANDOM_RANGE);
                } else if (preferences.contains(MyActivity.RANDOM_RANGE_KEY)) {
                    randomRange = preferences.getInt(MyActivity.RANDOM_RANGE_KEY, RANDOM_RANGE);
                }

                boolean lowRes = false;
                if (intent.hasExtra(EXTRA_LOWRES)) {
                    lowRes = intent.getBooleanExtra(EXTRA_LOWRES, false);
                } else if (preferences.contains(MyActivity.LOW_RES_KEY)) {
                    lowRes = preferences.getBoolean(MyActivity.LOW_RES_KEY, false);
                }

                changeWallpaper(tags, randomRange, lowRes);
            }
        }
    }

    private void publishProgress(String message) {
        publishProgress(message, true, 0);
    }

    private void publishProgress(String message, boolean indeterminate, int CurrentProgress) {
        notificationBuilder.setContentText(message);
        if (indeterminate) {
            notificationBuilder.setProgress(0, 0, true);
        } else {
            notificationBuilder.setProgress(100, CurrentProgress, false);
        }

        Notification notification = notificationBuilder.build();

        if (Build.VERSION.SDK_INT <= 10) {
            notification.setLatestEventInfo(this, getText(R.string.app_name), message, pendingIntent);
        }

        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void changeWallpaper(String tags, int randomRange, boolean lowRes) {

        long then = System.currentTimeMillis();

        Context context = getApplicationContext();

        publishProgress("Downloading wallpaper...");

        String photoId = getPhotoId(randomRange, tags);
        String photoPath = getPhotoPath(photoId, lowRes);

        URL url;
        try {
            url = new URL(photoPath);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            int total_size = connection.getContentLength();
            int current_size = 0;
            InputStream input = connection.getInputStream();
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            byte[] imageBuffer = new byte[65536 * 16];

            int read;
            while ((read = input.read(imageBuffer)) != -1) {
                current_size += read;
                output.write(imageBuffer, 0, read);
                publishProgress("Downloading wallpaper...", false, current_size * 100 / total_size);
            }

            publishProgress("Setting wallpaper as background...");

            Bitmap wallpaper = BitmapFactory.decodeByteArray(output.toByteArray(), 0, total_size);

            if (wallpaper != null) {
                setBitmapAsWallpaper(wallpaper, context, lowRes);
            } else {
                Log.w(TAG, "Wallpaper is null");
            }
            Log.d(MyActivity.TAG, "Service exited");
            stopForeground(true);
        } catch (IOException e) {
            Log.e(TAG, "An error occurred: " + e.toString());
        }

        Log.d(TAG, String.format("Total time: %fs", (((float)(System.currentTimeMillis() - then) / 1000))));
    }

    private String getPhotoId(int count, String tags) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;

        Gson gson = new Gson();

        getImageIdsUriBuilder
                .appendQueryParameter("tags", tags)
                .appendQueryParameter("api_key", MyActivity.API_KEY)
                .appendQueryParameter("per_page", String.valueOf(count));

        try {
            response = httpclient.execute(new HttpGet(getImageIdsUriBuilder.build().toString()));
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                String responseString = out.toString();

                JsonHelpers.RecentPhotosResponse resp = gson.fromJson(responseString, JsonHelpers.RecentPhotosResponse.class);

                int index = (int)Math.round(Math.random() * (resp.photos.photo.length - 1));

                return resp.photos.photo[index].id;
            } else{
                //Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (IOException e) {
            Log.d(TAG, e.toString());
            return null;
        }
    }

    private String getPhotoPath(String photoId, boolean lowRes) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;

        Gson gson = new Gson();

        getImageSizesUriBuilder
                .appendQueryParameter("api_key", MyActivity.API_KEY)
                .appendQueryParameter("photo_id", String.valueOf(photoId));

        try {
            response = httpclient.execute(new HttpGet(getImageSizesUriBuilder.build().toString()));
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                String responseString = out.toString();

                JsonHelpers.ImageSizesResponse resp = gson.fromJson(responseString, JsonHelpers.ImageSizesResponse.class);

                if (lowRes) {
                    return resp.sizes.size[1].source;
                } else {
                    return resp.sizes.size[resp.sizes.size.length - 1].source;
                }

            } else{
                //Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (IOException e) {
            Log.d(TAG, e.toString());
            return null;
        }
    }

    private void setBitmapAsWallpaper(Bitmap wallpaper, Context context, boolean lowRes) {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

        try {
            if (lowRes) {
                wallpaperManager.setBitmap(wallpaper);
            } else {
                Log.d(TAG, String.format("BEFORE: %d x %d", wallpaper.getWidth(), wallpaper.getHeight()));

                int width = wallpaper.getWidth();
                int height = wallpaper.getHeight();

                int desiredHeight = wallpaperManager.getDesiredMinimumHeight();
                int desiredWidth = width * desiredHeight / height;

                Log.d(TAG, String.format("AFTER: %d x %d", desiredWidth, desiredHeight));

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(wallpaper, desiredWidth, desiredHeight, true);
                wallpaperManager.setBitmap(scaledBitmap);
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }
}
