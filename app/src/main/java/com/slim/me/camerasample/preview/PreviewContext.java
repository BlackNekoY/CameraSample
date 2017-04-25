package com.slim.me.camerasample.preview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.slim.me.camerasample.camera.CameraHelper;

/**
 * Created by Slim on 2017/4/23.
 */
public class PreviewContext {

    public static final String TAG = "PreviewContext";

    protected Context context;

    public PreviewContext(@NonNull Context context) {
        this.context = context;
    }

    protected void getPreviewFrame(byte[] data) {
        CameraHelper.getInstance().addUserBufferRecycle(data);
    }
}
