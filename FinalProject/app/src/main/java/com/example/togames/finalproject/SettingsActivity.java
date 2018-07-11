package com.example.togames.finalproject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioGroup;

public class SettingsActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener,
        View.OnClickListener {

    private RadioGroup radioGroup_theme, radioGroup_language;
    private boolean isDark;
    private ImageButton imageButton_settings_back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        isDark = AppSettings.getInstance(this).isDarkTheme;
        setTheme(isDark ? R.style.NoTitleThemeDark : R.style.NoTitleTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        imageButton_settings_back = findViewById(R.id.imageButton_settings_back);
        radioGroup_theme = findViewById(R.id.radioGroup_theme);
        radioGroup_language = findViewById(R.id.radioGroup_language);
        radioGroup_theme.check(isDark ? R.id.radioButton_theme_dark : R.id.radioButton_theme_light);
        radioGroup_language.check(R.id.radioButton_language_en);
        radioGroup_theme.setOnCheckedChangeListener(this);
        radioGroup_language.setOnCheckedChangeListener(this);
        imageButton_settings_back.setOnClickListener(this);

        Intent intentThemeReturn = new Intent();
        intentThemeReturn.putExtra(MainActivity.THEME_PICK, isDark);
        setResult(MainActivity.REQUEST_THEME, intentThemeReturn);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.imageButton_settings_back:
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        int id = radioGroup.getId();
        switch (id) {
            case R.id.radioGroup_theme:
                isDark = (i == R.id.radioButton_theme_dark);
                AppSettings.getInstance(this).setDark(isDark);
                setTheme(isDark ? R.style.NoTitleThemeDark : R.style.NoTitleTheme);
                recreate();
                break;
            case R.id.radioGroup_language:
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(AppSettings.SAVED_THEME, isDark);
        editor.apply();
    }
}
