package org.catinthedark.wallpepper;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.catinthedark.wallpepper.asynctask.RequestTask;


public class MyActivity extends Activity {

    public class Response {
        public ImgSrc[] response;
    }

    public class ImgSrc {
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

    public static final String TAG = "WallPepper";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        Button setWallpaperButton = (Button) findViewById(R.id.setWallpaperButton);

        setWallpaperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String groupId = ((EditText) findViewById(R.id.editText)).getText().toString();

                String url = String.format("https://api.vk.com/method/photos.get?owner_id=-%s&album_id=wall&rev=1&count=10", groupId);

                new RequestTask().execute(url, getApplicationContext());
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
