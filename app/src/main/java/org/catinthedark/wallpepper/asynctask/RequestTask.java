package org.catinthedark.wallpepper.asynctask;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
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

                MyActivity.Response resp = gson.fromJson(responseString, MyActivity.Response.class);

                int index = (int)Math.round(Math.random() * (resp.response.length - 1));

                if (index >= 0) {
                    MyActivity.ImgSrc image = resp.response[index];
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
        Log.d(MyActivity.TAG, "Here");
        try {
            if (wallpaper != null) {
                WallpaperManager.getInstance(context).setBitmap(wallpaper);
                WallpaperManager.getInstance(context).setWallpaperOffsetSteps(1, 0);
            } else {
                Log.w(MyActivity.TAG, "Wallpaper is null, try again");
            }
        } catch (IOException e) {
            Log.e(MyActivity.TAG, e.toString());
        }
    }
}
