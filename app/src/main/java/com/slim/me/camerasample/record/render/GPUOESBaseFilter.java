package com.slim.me.camerasample.record.render;

import android.content.Context;
import android.opengl.GLES11Ext;

import com.slim.me.camerasample.util.FilterUtil;

/**
 * Created by slimxu on 2018/1/14.
 */

public class GPUOESBaseFilter extends GPUBaseFilter {
    public static final String OES_FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +      // highp here doesn't seem to matter
                    "varying vec2 textureCoordinate;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(sTexture, textureCoordinate);\n" +
                    "}\n";


    public GPUOESBaseFilter() {
        this(null);
    }

    public GPUOESBaseFilter(Context context){
        this(context, FilterUtil.NO_FILTER_VERTEX_SHADER, OES_FRAGMENT_SHADER);
    }

    public GPUOESBaseFilter(Context context, final String vertexShader, final String fragmentShader){
        super(context, vertexShader, fragmentShader);
        mTextureType = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
    }
}
