package org.catinthedark.wallpepper.service;

import android.app.IntentService;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.catinthedark.wallpepper.MyActivity;
import org.catinthedark.wallpepper.R;
import org.catinthedark.wallpepper.WallpepperNotification;
import org.catinthedark.wallpepper.json.JsonHelpers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WallpaperService extends IntentService {
    public static final String ACTION_CHANGE_WALLPAPER = "org.catinthedark.wallpepper.service.action.CHANGE_WALLPAPER";
    public static final String ACTION_SAVE_CURRENT_WALLPAPER = "org.catinthedark.wallpepper.service.action.SAVE_CURRENT_WALLPAPER";
    public static final String EXTRA_TAGS = "org.catinthedark.wallpepper.service.extra.TAGS";
    public static final String EXTRA_LOWRES = "org.catinthedark.wallpepper.service.extra.LOWRES";
    public static final String EXTRA_RANDOM_RANGE = "org.catinthedark.wallpepper.service.extra.RANDOM_RANGE";
    public static final String EXTRA_LAST_SAVED_WALLPAPER = "org.catinthedark.wallpepper.service.extra.LAST_SAVED_WALLPAPER";

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

    private final int RANDOM_RANGE = 15;
    private WallpepperNotification wallpepperNotification;

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

    public static void startSaveCurrentWallpaper(Context context, String lastWallpaperPath) {
        Intent intent = new Intent(context, WallpaperService.class);
        intent.setAction(ACTION_SAVE_CURRENT_WALLPAPER);
        intent.putExtra(EXTRA_LAST_SAVED_WALLPAPER, lastWallpaperPath);
        context.startService(intent);
    }

    public WallpaperService() {
        super("WallpaperService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Context context = getApplicationContext();
        wallpepperNotification = new WallpepperNotification(context, getText(R.string.app_name));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        startForeground(WallpepperNotification.NOTIFICATION_ID, wallpepperNotification.getNotification());
        return START_NOT_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_CHANGE_WALLPAPER.equals(action)) {
                long then = System.currentTimeMillis();

                SharedPreferences preferences = getSharedPreferences(MyActivity.SHARED_PREFS_NAME, MODE_PRIVATE);

                String tags = "";
                if (intent.hasExtra(EXTRA_TAGS)) {
                    tags = intent.getStringExtra(EXTRA_TAGS);
                } else if (preferences.contains(MyActivity.TAGS_KEY)) {
                    tags = preferences.getString(MyActivity.TAGS_KEY, "");
                }

                if (tags == null || tags.isEmpty()) {
                    Context context = getApplicationContext();
                    getApplicationContext().startActivity(
                            new Intent(context, MyActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    return;
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

                Log.d(MyActivity.TAG, "Service exited");
                stopForeground(true);

                Log.d(TAG, String.format("Total time: %fs", (((float) (System.currentTimeMillis() - then) / 1000))));
            } else if (action.equals(ACTION_SAVE_CURRENT_WALLPAPER)) {
                if (intent.hasExtra(EXTRA_LAST_SAVED_WALLPAPER)) {
                    String lastWallpaperPath = intent.getStringExtra(EXTRA_LAST_SAVED_WALLPAPER);
                    File pictureFolder = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES
                    );
                    File wallpepperFolder = new File(pictureFolder, "Wallpepper");
                    if (wallpepperFolder.mkdirs() || wallpepperFolder.isDirectory()) {
                        long millis = System.currentTimeMillis();
                        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy_HH:mm", getResources().getConfiguration().locale);
                        Date currentDate = new Date(millis);
                        String filename = String.format("Wallpepper_%s.png", sdf.format(currentDate));

                        File outputFile = new File(wallpepperFolder, filename);

                        downloadPhotoToFile(lastWallpaperPath, outputFile);

                        Log.d(MyActivity.TAG, "Service exited");
                        stopForeground(true);

                    } else {
                        Toast.makeText(
                                getApplicationContext(),
                                "Unable to save current background: Unable to create directory", Toast.LENGTH_LONG
                        ).show();
                    }
                }
            }
        }
    }

    private void changeWallpaper(String tags, int randomRange, boolean lowRes) {
        Context context = getApplicationContext();

        wallpepperNotification.publishProgress(getText(R.string.download_bg_progress).toString());

        String photoId = getPhotoId(randomRange, tags);
        String photoPath = getPhotoPath(photoId, lowRes);

        String progressTitle = getText(R.string.download_bg_progress).toString();
        Bitmap wallpaper = downloadBitmap(photoPath, wallpepperNotification, progressTitle);

        wallpepperNotification.publishProgress(getString(R.string.set_bg_progress));

        if (wallpaper != null) {
            if (setBitmapAsWallpaper(wallpaper, context, lowRes)) {
                SharedPreferences preferences = getSharedPreferences(MyActivity.SHARED_PREFS_NAME, MODE_PRIVATE);
                preferences.edit().putString(MyActivity.LAST_WALLPAPER_URL_KEY, photoPath).apply();
            }
        } else {
            Log.w(TAG, "Wallpaper is null");
        }
    }

    private void downloadPhotoToFile(String url, File outFile) {
        FileOutputStream out;
        try {
            out = new FileOutputStream(outFile);
            Bitmap bitmap = downloadBitmap(url, wallpepperNotification, getText(R.string.save_background).toString());

            wallpepperNotification.publishProgress(getString(R.string.save_to_file));

            if (bitmap != null) {
                try {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Bitmap downloadBitmap(String bitmapPath, WallpepperNotification progressNotification, String notificationTitle) {
        try {
            URL url = new URL(bitmapPath);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            int total_size = connection.getContentLength();
            int current_size = 0;
            InputStream input = connection.getInputStream();
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            byte[] imageBuffer = new byte[65536 * 16];

            long then = System.currentTimeMillis();

            int read;
            while ((read = input.read(imageBuffer)) != -1) {
                current_size += read;
                output.write(imageBuffer, 0, read);
                if (System.currentTimeMillis() - then >= 250) {
                    progressNotification.publishProgress(
                            notificationTitle,
                            false, current_size * 100 / total_size);
                    then = System.currentTimeMillis();
                }
            }
            return BitmapFactory.decodeByteArray(output.toByteArray(), 0, total_size);
        } catch (IOException e) {
            Log.e(TAG, "An error occurred: " + e.toString());
            return null;
        }
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

    private boolean setBitmapAsWallpaper(Bitmap wallpaper, Context context, boolean lowRes) {
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
            return true;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }
}
