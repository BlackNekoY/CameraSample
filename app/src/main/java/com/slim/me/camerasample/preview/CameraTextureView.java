package com.slim.me.camerasample.preview;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

/**
 * Created by Slim on 2017/3/19.
 */

public class CameraTextureView extends TextureView implements TextureView.SurfaceTextureListener {

    private static final String TAG = "CameraTextureView";

    private TexturePreviewContext mPreviewContext;

    public CameraTextureView(Context context) {
        this(context, null);
    }

    public CameraTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setSurfaceTextureListener(this);
    }

    public void setPreviewContext(TexturePreviewContext context) {
        this.mPreviewContext = context;
    }

    public TexturePreviewContext getPreviewContext() {
        return mPreviewContext;
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureAvailable");
        if(mPreviewContext != null) {
            mPreviewContext.onSurfaceTextureAvailable(surface, width, height);
        }

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureSizeChanged");
        if(mPreviewContext != null) {
            mPreviewContext.onSurfaceTextureSizeChanged(surface, width, height);
        }

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureDestroyed");
        if(mPreviewContext != null) {
            return mPreviewContext.onSurfaceTextureDestroyed(surface);
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureUpdated");
        if(mPreviewContext != null) {
            mPreviewContext.onSurfaceTextureUpdated(surface);
        }
    }

}
