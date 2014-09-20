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

    private class Response {
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

    @Override
    protected Bitmap doInBackground(Object... params) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        context = (Context) params[1];
        try {
            response = httpclient.execute(new HttpGet((String) params[0]));
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                String responseString = out.toString();

                Gson gson = new Gson();

                Response resp = gson.fromJson(responseString, Response.class);

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
                    Log.w(MyActivity.TAG, "Public has not wallpapers");
                    return null;
                }
            } else{
                //Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (Exception e) {
            Log.d(MyActivity.TAG, e.toString());
            return null;
        }
    }

    @Override
    protected void onPostExecute(Bitmap wallpaper) {
        try {
            if (wallpaper != null) {
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

                Log.d(MyActivity.TAG, String.format("BEFORE: %d x %d", wallpaper.getWidth(), wallpaper.getHeight()));

                int height = wallpaper.getHeight();
                int desiredHeight = wallpaperManager.getDesiredMinimumHeight();
                int desiredWidth = wallpaper.getWidth() * desiredHeight / height;

                Log.d(MyActivity.TAG, String.format("AFTER: %d x %d", desiredWidth, desiredHeight));

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(wallpaper, desiredWidth, desiredHeight, true);
                wallpaperManager.setBitmap(scaledBitmap);
                Toast.makeText(context, "Done!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "An error occured, try again", Toast.LENGTH_SHORT).show();
                Log.w(MyActivity.TAG, "Wallpaper is null, try again");
            }
        } catch (IOException e) {
            Log.e(MyActivity.TAG, e.toString());
        }
    }
}
