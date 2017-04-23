package com.slim.me.camerasample.util;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.Log;
import android.view.WindowManager;

import java.lang.reflect.Field;

/**
 * Created by slimxu on 2017/4/16.
 */

public class UIUtil {

    private static final String TAG = "UIUtil";
    public static int sStatusBarHeight = -1;
    public static int sWindowWidth = -1;
    public static int sWindowHeight = -1;

    public static float getDensity(Context context) {
        return context.getResources().getDisplayMetrics().density;
    }

    public static int dip2px(Context context, float dip) {
        final float scale = getDensity(context);
        return (int) (dip * scale + 0.5f);
    }

    public static float px2dip(Context context, float px) {
        final float scale = getDensity(context);
        return (px / scale + 0.5f);
    }

    public static int getWindowScreenWidth(Context context) {
        if (sWindowWidth > 0) {
            return sWindowWidth;
        }

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point size = new Point();
            wm.getDefaultDisplay().getSize(size);
            sWindowWidth = size.x;
        } else {
            sWindowWidth = wm.getDefaultDisplay().getWidth();
        }

        return sWindowWidth;
    }

    public static float getWindowScreenWidthDP(Context contex){
        return px2dip(contex, getWindowScreenWidth(contex));
    }

    public static int getWindowScreenHeight(Context context) {
        if (sWindowHeight > 0) {
            return sWindowHeight;
        }

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point size = new Point();
            wm.getDefaultDisplay().getSize(size);
            sWindowHeight = size.y;
        } else {
            sWindowHeight = wm.getDefaultDisplay().getHeight();
        }

        return sWindowHeight;
    }

//    public static int getPix(Context context, int res) {
//        return context.getResources().getDimensionPixelSize(res);
//    }

    public static int getStatusBarHeight(Context ctx) {
        if (sStatusBarHeight != -1) {
            return sStatusBarHeight;
        }
        int height = -1;
        try {
            Class c = Class.forName("com.android.internal.R$dimen");
            Field field = c.getField("status_bar_height");
            int id = (Integer) field.get(null);
            height = ctx.getResources().getDimensionPixelSize(id);
        } catch (Exception e) {
            Log.w(TAG, e.toString());
        }
        if (height <= 0) { // 获取失败, 使用Android源码的默认值25dp
            height = dip2px(ctx, 25);
        }
        return sStatusBarHeight = height;
    }
}
