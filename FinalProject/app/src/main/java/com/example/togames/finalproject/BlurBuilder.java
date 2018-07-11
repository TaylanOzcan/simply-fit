package com.example.togames.finalproject;

import android.content.Context; import android.graphics.Bitmap; import android.support.v8.renderscript.Allocation; import android.support.v8.renderscript.Element; import android.support.v8.renderscript.RenderScript; import android.support.v8.renderscript.ScriptIntrinsicBlur;

/**  * Created by ssaurel on 29/09/2016.  */ public class BlurBuilder {

    private static final float BITMAP_SCALE = 0.6f;
    private static final float BLUR_RADIUS = 4f;

    public static Bitmap blur(Context context, Bitmap image) {
        int width = Math.round(image.getWidth() * BITMAP_SCALE);
        int height = Math.round(image.getHeight() * BITMAP_SCALE);

        Bitmap inputBitmap = Bitmap.createScaledBitmap(image, width, height, false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

        RenderScript rs = RenderScript.create(context);

        ScriptIntrinsicBlur intrinsicBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
        Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);

        intrinsicBlur.setRadius(BLUR_RADIUS);
        intrinsicBlur.setInput(tmpIn);
        intrinsicBlur.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);

        return outputBitmap;
    }

}