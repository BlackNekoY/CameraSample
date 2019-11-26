package com.slim.me.camerasample.util;

import android.content.Context;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.widget.Toast;


public class ToastUtils {

    private static int SHOW_INTERVAL = 2500;

    private static String sLastStr;
    private static long sLastToastTime = 0L;


    private ToastUtils() {
    }

    public static void showToast(Context context, @StringRes int resId) {
        showToast(context, context.getString(resId));
    }

    public static void showToast(Context context, String text) {
        showToast(context, text, Toast.LENGTH_SHORT);
    }

    public static void showToast(Context context, String message, int length) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        context = context.getApplicationContext();
        int len = length == Toast.LENGTH_SHORT ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
        if (!message.equals(sLastStr) || System.currentTimeMillis() - sLastToastTime >= SHOW_INTERVAL) {
            Toast.makeText(context, message, len).show();
            sLastStr = message;
            sLastToastTime = System.currentTimeMillis();
        }
    }
}