package org.catinthedark.wallpepper;

import android.app.Activity;
import android.content.SharedPreferences;
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

    private final String SHARED_PREFS_NAME = "wallpepper_sharedprefs";
    private final String GROUP_ID_KEY = "group_id";
    private final String RANDOM_RANGE_KEY = "random_range";

    private EditText groupIdEditText;
    private EditText randomRangeEditText;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        groupIdEditText = (EditText) findViewById(R.id.groupIdEditText);
        randomRangeEditText = (EditText) findViewById(R.id.randomRangeEditText);
        Button setWallpaperButton = (Button) findViewById(R.id.setWallpaperButton);

        preferences = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);
        if (preferences.contains(GROUP_ID_KEY)) {
            groupIdEditText.setText(preferences.getString(GROUP_ID_KEY, ""));
        }

        if (preferences.contains(RANDOM_RANGE_KEY)) {
            randomRangeEditText.setText(String.valueOf(preferences.getInt(RANDOM_RANGE_KEY, 10)));
        }

        setWallpaperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String groupId = groupIdEditText.getText().toString();
                int randomNumber = Integer.valueOf(randomRangeEditText.getText().toString());

                String url = String.format("https://api.vk.com/method/photos.get?owner_id=-%s&album_id=wall&rev=1&count=%d", groupId, randomNumber);

                new RequestTask().execute(url, getApplicationContext());
            }
        });
    }

    @Override
    protected void onPause() {
        preferences.edit()
                .putInt(RANDOM_RANGE_KEY, Integer.valueOf(randomRangeEditText.getText().toString()))
                .putString(GROUP_ID_KEY, groupIdEditText.getText().toString())
                .apply();
        super.onPause();
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
