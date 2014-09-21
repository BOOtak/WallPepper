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

    public static final String TAG = "WallPepper";

    public static final String API_KEY = "2cc3659a84bb322d7523a89e53d58578";

    private final String SHARED_PREFS_NAME = "wallpepper_sharedprefs";
    private final String TAGS_KEY = "tags";
    private final String RANDOM_RANGE_KEY = "random_range";

    private EditText randomRangeEditText;
    private EditText tagsEditText;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        randomRangeEditText = (EditText) findViewById(R.id.randomRangeEditText);
        tagsEditText = (EditText) findViewById(R.id.tagsEditText);
        Button setWallpaperButton = (Button) findViewById(R.id.setWallpaperButton);

        preferences = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);

        if (preferences.contains(RANDOM_RANGE_KEY)) {
            randomRangeEditText.setText(String.valueOf(preferences.getInt(RANDOM_RANGE_KEY, 10)));
        }

        if (preferences.contains(TAGS_KEY)) {
            tagsEditText.setText(preferences.getString(TAGS_KEY, ""));
        }

        setWallpaperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int randomNumber = Integer.valueOf(randomRangeEditText.getText().toString());
                String tags = tagsEditText.getText().toString();

                new RequestTask().execute(getApplicationContext(), randomNumber, tags);
            }
        });
    }

    @Override
    protected void onPause() {
        preferences.edit()
                .putInt(RANDOM_RANGE_KEY, Integer.valueOf(randomRangeEditText.getText().toString()))
                .putString(TAGS_KEY, tagsEditText.getText().toString())
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
