package com.example.togames.finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;

import java.util.ArrayList;

public class GalleryActivity extends AppCompatActivity implements View.OnClickListener,
        GridView.OnItemClickListener {

    static final String IMAGE_PATHS = "image_paths";
    private ArrayList<String> paths;
    private GridView gridView;
    private ImageButton imageButton_gallery_back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme((AppSettings.getInstance(this).isDarkTheme) ?
                R.style.NoTitleThemeDark : R.style.NoTitleTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        paths = getIntent().getExtras().getStringArrayList(IMAGE_PATHS);
        if (paths == null) finish();

        imageButton_gallery_back = findViewById(R.id.imageButton_gallery_back);
        gridView = findViewById(R.id.gridView);
        gridView.setAdapter(new ImageAdapter(this, paths));
        gridView.setOnItemClickListener(this);
        imageButton_gallery_back.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.imageButton_gallery_back:
                // Finish current activity
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Intent intent = new Intent();
        intent.putExtra(MainActivity.GALLERY_PICK, paths.get(i));
        setResult(MainActivity.REQUEST_PICK_PHOTO, intent);
        finish();
    }
}
