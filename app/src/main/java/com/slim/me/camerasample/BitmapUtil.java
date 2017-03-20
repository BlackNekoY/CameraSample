package com.slim.me.camerasample;

import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * Created by Slim on 2017/3/19.
 */

public class BitmapUtil {

    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.postRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

}
