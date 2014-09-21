package org.catinthedark.wallpepper.asynctask;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by kirill on 17.09.14.
 */
public class RequestTask extends AsyncTask<Object, Void, Bitmap> {

    private class RecentPhotosResponse {
        public PhotoPageInfo photos;
        public String stat;
    }

    private class PhotoPageInfo {
        public int page;
        public int pages;
        public int perpage;
        public String total;
        public Photo[] photo;
    }

    private class Photo {
        public String id;
        public String owner;
        public String sercet;
        public String server;
        public int farm;
        public String title;
        public int ispublic;
        public int isfriend;
        public int isfamily;
    }

    private class ImageSizesResponse {
        public Sizes sizes;
        public String stat;
    }

    private class Sizes {
        public Size[] size;
    }

    private class Size {
        public String label;
        public String width;
        public String height;
        public String source;
        public String url;
        public String media;
    }

    Context context;
    int count;
    String tags;

    private static final String TAG = MyActivity.TAG;

    private final String getImageIdsUrlFormat = "https://api.flickr.com/services/rest/?method=flickr.photos.search&api_key=%s&tags=%s&per_page=%d&page=1&format=json&nojsoncallback=1";
    private final String getImageSizesUrlFormat = "https://api.flickr.com/services/rest/?method=flickr.photos.getSizes&api_key=%s&photo_id=%s&format=json&nojsoncallback=1";

    @Override
    protected Bitmap doInBackground(Object... params) {
        context = (Context) params[0];
        count = (Integer) params[1];
        tags = (String) params[2];

        String photoId = getPhotoId(count);
        String photoPath = getPhotoPath(photoId);

        URL url;
        try {
            url = new URL(photoPath);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();

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

                RecentPhotosResponse resp = gson.fromJson(responseString, RecentPhotosResponse.class);

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

                ImageSizesResponse resp = gson.fromJson(responseString, ImageSizesResponse.class);

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
            } else {
                Toast.makeText(context, "An error occured, try again", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Wallpaper is null, try again");
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }
}
