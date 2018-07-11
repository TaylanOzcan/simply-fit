package com.example.togames.finalproject;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.Window;
import android.widget.ImageView;

public class ImageDialog extends Dialog implements BitmapChangeListener {

    private ImageView imageView_picture;
    private Context context;

    public ImageDialog(Context context, Bitmap bitmap) {
        super(context);

        this.context = context;
        setCancelable(false);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_image);

        imageView_picture = findViewById(R.id.imageView_picture);
        imageView_picture.setImageBitmap(bitmap);

        int width = (context.getResources().getDisplayMetrics().widthPixels);
        int height = (context.getResources().getDisplayMetrics().heightPixels);
        int min = Math.min(width, height);
        getWindow().setLayout(min, min + 400);
    }

    public void placeBitmap(Bitmap bitmap){
        imageView_picture.setImageBitmap(bitmap);
    }

    @Override
    public void onBitmapChange(Bitmap bitmap) {
        placeBitmap(bitmap);
    }
}
