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

    private class GroupIdResponse {
        public GroupId[] response;
    }

    private class GroupId {
        public int gid;
        public String name;
        public String screen_name;
        public int is_closed;
        public String type;
        public int is_admin;
        public int is_member;
        public String photo_50;
        public String photo_100;
        public String photo_200;
    }

    private class ImagesResponse {
        public ImgSrc[] response;
    }

    private class ImgSrc {
        public int pid;
        public int aid;
        public int owner_id;
        public int user_id;
        public String src;
        public String src_big;
        public String src_small;
        public String src_xbig;
        public String src_xxbig;
        public int width;
        public int height;
        public String text;
        public long created;
        public int post_id;
    }

    Context context;
    int count;
    String groupName;

    private static final String TAG = MyActivity.TAG;

    private final String getImageUrlFormat = "https://api.vk.com/method/photos.get?owner_id=-%d&album_id=wall&rev=1&count=%d";
    private final String getGroupIdUrlFormat = "http://api.vk.com/method/groups.getById?group_id=%s";

    @Override
    protected Bitmap doInBackground(Object... params) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        context = (Context) params[0];
        groupName = (String) params[1];
        count = (Integer) params[2];

        Gson gson = new Gson();

        int groupId;

        try {
            response = httpclient.execute(new HttpGet(String.format(getGroupIdUrlFormat, groupName)));
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                String responseString = out.toString();

                GroupIdResponse resp = gson.fromJson(responseString, GroupIdResponse.class);

                groupId = resp.response[0].gid;
            } else {
                Log.d(TAG, "Unable to complete HTTP request: " + statusLine.getReasonPhrase());
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to execute HTTP client: " + e.getMessage());
            return null;
        }

        if (groupId <= 0) {
            Log.e(TAG, "Unable to resolve group Id by name");
            return null;
        }

        try {
            response = httpclient.execute(new HttpGet(String.format(getImageUrlFormat, groupId, count)));
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                String responseString = out.toString();

                ImagesResponse resp = gson.fromJson(responseString, ImagesResponse.class);

                int index = (int)Math.round(Math.random() * (resp.response.length - 1));

                if (index >= 0) {
                    ImgSrc image = resp.response[index];
                    String imgsrc = "";
                    if (image.src_xxbig != null) {
                        imgsrc = image.src_xxbig;
                    } else if (image.src_xbig != null) {
                        imgsrc = image.src_xbig;
                    } else if (image.src_big != null) {
                        imgsrc = image.src_big;
                    } else if (image.src_small != null) {
                        imgsrc = image.src_small;
                    } else {
                        imgsrc = image.src;
                    }

                    URL url = new URL(imgsrc);

                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();

                    return BitmapFactory.decodeStream(input);
                } else {
                    Log.w(TAG, "Public has not wallpapers");
                    return null;
                }
            } else{
                //Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (Exception e) {
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
                int desiredWidth;
                int desiredHeight;

                if (width > height) {
                    desiredWidth = wallpaperManager.getDesiredMinimumWidth();
                    desiredHeight = height * desiredWidth / width;
                } else {
                    desiredHeight = wallpaperManager.getDesiredMinimumHeight();
                    desiredWidth = width * desiredHeight / height;
                }

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
