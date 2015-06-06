package org.catinthedark.wallpepper;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.catinthedark.wallpepper.service.WallpaperService;


public class MyActivity extends ActionBarActivity implements TextWatcher {

    public static final String TAG = "WallPepper";

    public static final String API_KEY = "2cc3659a84bb322d7523a89e53d58578";

    public static final String SHARED_PREFS_NAME = "wallpepper_sharedprefs";
    public static final String TAGS_KEY = "tags";
    public static final String LOW_RES_KEY = "low_res";
    public static final String RANDOM_RANGE_KEY = "random_range";
    public static final String LAST_WALLPAPER_URL_KEY = "last_wallpaper_url";

    private EditText randomRangeEditText;
    private EditText tagsEditText;
    private CheckBox lowResCheckBox;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        randomRangeEditText = (EditText) findViewById(R.id.randomRangeEditText);
        tagsEditText = (EditText) findViewById(R.id.tagsEditText);
        lowResCheckBox = (CheckBox) findViewById(R.id.lowResCheckBox);
        Button setWallpaperButton = (Button) findViewById(R.id.setWallpaperButton);

        tagsEditText.addTextChangedListener(this);

        preferences = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);

        if (preferences.contains(RANDOM_RANGE_KEY)) {
            randomRangeEditText.setText(String.valueOf(preferences.getInt(RANDOM_RANGE_KEY, 10)));
        }

        if (preferences.contains(TAGS_KEY)) {
            tagsEditText.setText(preferences.getString(TAGS_KEY, ""));
        }

        if (preferences.contains(LOW_RES_KEY)) {
            lowResCheckBox.setChecked(preferences.getBoolean(LOW_RES_KEY, false));
        }

        setWallpaperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tagsEditText.getText().toString().isEmpty()) {
                    Toast.makeText(
                            getApplicationContext(), "Please fill tags", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    int randomNumber = Integer.valueOf(randomRangeEditText.getText().toString());
                    String tags = tagsEditText.getText().toString();
                    boolean lowRes = lowResCheckBox.isChecked();

                    WallpaperService.startChangeWallpaper(getApplicationContext(), tags, randomNumber, lowRes);
                }
            }
        });

        Button saveCurrantWallpaperButton = (Button) findViewById(R.id.saveCurrentWallpaperButton);

        saveCurrantWallpaperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences preferences = getSharedPreferences(MyActivity.SHARED_PREFS_NAME, MODE_PRIVATE);

                if (preferences.contains(MyActivity.LAST_WALLPAPER_URL_KEY)) {
                    String lastWallpaperPath = preferences.getString(MyActivity.LAST_WALLPAPER_URL_KEY, "");
                    if (lastWallpaperPath != null && !lastWallpaperPath.isEmpty()) {
                        WallpaperService.startSaveCurrentWallpaper(getApplicationContext(), lastWallpaperPath);
                    }
                } else {
                    Toast.makeText(
                            getApplicationContext(),
                            "No wallpapers were set by Wallpepper! Try set wallpaper.", Toast.LENGTH_LONG
                    ).show();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        preferences.edit()
                .putInt(RANDOM_RANGE_KEY, Integer.valueOf(randomRangeEditText.getText().toString()))
                .putString(TAGS_KEY, tagsEditText.getText().toString())
                .putBoolean(LOW_RES_KEY, lowResCheckBox.isChecked())
                .apply();
        super.onPause();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        String filtered_tags = s.toString();
        if (filtered_tags.matches(".*[^a-z^0-9,].*")) {

            filtered_tags = filtered_tags.replaceAll("[^a-z^0-9,]", "");
            s.clear();
            s.append(filtered_tags);
        }
    }
}
