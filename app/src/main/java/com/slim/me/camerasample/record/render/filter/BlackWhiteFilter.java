package com.slim.me.camerasample.record.render.filter;

import android.support.annotation.NonNull;

import com.slim.me.camerasample.R;
import com.slim.me.camerasample.util.OpenGLUtils;

public class BlackWhiteFilter extends NoEffectFilter {

    @Override
    protected void onDrawFrame(int textureId, float[] cameraMatrix, float[] textureMatrix) {
        super.onDrawFrame(textureId, cameraMatrix, textureMatrix);
    }

    @NonNull
    @Override
    public String getFragmentShader() {
        return OpenGLUtils.readShaderFromRawResource(R.raw.blackwhite);
    }
}
