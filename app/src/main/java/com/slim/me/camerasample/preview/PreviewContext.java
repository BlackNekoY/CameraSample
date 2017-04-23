package com.slim.me.camerasample.preview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Created by Slim on 2017/4/23.
 */
public class PreviewContext {

    public static final String TAG = "PreviewContext";

    protected Context context;

    public PreviewContext(@NonNull Context context) {
        this.context = context;
    }

    void getPreviewFrame(byte[] data) {
        Log.d(TAG, "getPreviewFrame");
    }
}
