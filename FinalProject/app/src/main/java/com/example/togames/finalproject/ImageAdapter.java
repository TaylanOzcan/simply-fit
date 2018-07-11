package com.example.togames.finalproject;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ImageAdapter extends BaseAdapter {
    private static final int PADDING = 8;
    private Context mContext;
    private ArrayList<String> paths;
    private int columnWidth;

    public ImageAdapter(Context c, ArrayList<String> paths) {
        mContext = c;
        this.paths = paths;

        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).
                getDefaultDisplay().getMetrics(metrics);
        columnWidth = metrics.widthPixels / 3 - (4 * PADDING);
    }

    public int getCount() {
        return paths.size();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(columnWidth, columnWidth));
            //imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setPadding(PADDING, PADDING, PADDING, PADDING);
        } else {
            imageView = (ImageView) convertView;
        }

        Picasso.with(mContext).load("file://" + paths.get(position)).config(Bitmap.Config.RGB_565)
                .fit().centerCrop()
                .into(imageView);
        return imageView;
    }

}