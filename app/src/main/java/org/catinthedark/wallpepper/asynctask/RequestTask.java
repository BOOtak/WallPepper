package org.catinthedark.wallpepper.asynctask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
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
import org.catinthedark.wallpepper.json.JsonHelpers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by kirill on 17.09.14.
 */
public class RequestTask extends AsyncTask<Object, String, Bitmap> {

    Context context;
    int count;
    String tags;

    private static final String TAG = MyActivity.TAG;

    private final String getImageIdsUrlFormat = "https://api.flickr.com/services/rest/?method=flickr.photos.search&api_key=%s&tags=%s&per_page=%d&page=1&format=json&nojsoncallback=1";
    private final String getImageSizesUrlFormat = "https://api.flickr.com/services/rest/?method=flickr.photos.getSizes&api_key=%s&photo_id=%s&format=json&nojsoncallback=1";

    NotificationManager notificationManager;
    Notification.Builder notificationBuilder;

    @Override
    protected Bitmap doInBackground(Object... params) {
        context = (Context) params[0];
        count = (Integer) params[1];
        tags = (String) params[2];

        notificationBuilder = new Notification.Builder(context)
                .setSmallIcon(android.R.drawable.ic_menu_gallery)
                .setContentTitle("Setting background...")
                .setProgress(0, 0, true);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        publishProgress("Downloading wallpaper...");

        String photoId = getPhotoId(count);
        String photoPath = getPhotoPath(photoId);
        URL url;
        try {
            url = new URL(photoPath);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();

            publishProgress("Setting wallpaper as background...");
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            Log.e(TAG, "An error occurred: " + e.toString());
            return null;
        }
    }

    private String getPhotoId(int count) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;

        Gson gson = new Gson();

        try {
            response = httpclient.execute(new HttpGet(String.format(getImageIdsUrlFormat, MyActivity.API_KEY, tags, count)));
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

    private String getPhotoPath(String photoId) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;

        Gson gson = new Gson();

        try {
            response = httpclient.execute(new HttpGet(String.format(getImageSizesUrlFormat, MyActivity.API_KEY, photoId)));
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                String responseString = out.toString();

                JsonHelpers.ImageSizesResponse resp = gson.fromJson(responseString, JsonHelpers.ImageSizesResponse.class);

                return resp.sizes.size[resp.sizes.size.length - 1].source;

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

    @Override
    protected void onProgressUpdate(String... values) {
        notificationBuilder.setContentText(values[0]);
        notificationManager.notify(0, notificationBuilder.build());
    }

    @Override
    protected void onPostExecute(Bitmap wallpaper) {
        try {
            if (wallpaper != null) {
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

                Log.d(TAG, String.format("BEFORE: %d x %d", wallpaper.getWidth(), wallpaper.getHeight()));

                int width = wallpaper.getWidth();
                int height = wallpaper.getHeight();

                int desiredHeight = wallpaperManager.getDesiredMinimumHeight();
                int desiredWidth = width * desiredHeight / height;

                Log.d(TAG, String.format("AFTER: %d x %d", desiredWidth, desiredHeight));

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(wallpaper, desiredWidth, desiredHeight, true);
                wallpaperManager.setBitmap(scaledBitmap);
                Toast.makeText(context, "Done!", Toast.LENGTH_SHORT).show();
                notificationManager.cancel(0);
            } else {
                Toast.makeText(context, "An error occured, try again", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Wallpaper is null, try again");
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }
}
