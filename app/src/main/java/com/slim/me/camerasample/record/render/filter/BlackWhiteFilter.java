package com.slim.me.camerasample.record.render.filter;

import android.support.annotation.NonNull;

import com.slim.me.camerasample.R;
import com.slim.me.camerasample.util.OpenGLUtils;

public class BlackWhiteFilter extends GPUImageFilter {

    @NonNull
    @Override
    public String getFragmentShader() {
        return OpenGLUtils.readShaderFromRawResource(R.raw.blackwhite);
    }
}
