package com.slim.me.camerasample.camera_mpeg;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

/**
 * Created by slimxu on 2018/1/10.
 */

public class CameraGLSurfaceView extends GLSurfaceView implements SurfaceTexture.OnFrameAvailableListener {

    private GLSurfaceViewRender mRender;

    public CameraGLSurfaceView(Context context) {
        super(context);
        init();
    }

    public CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);
        mRender = new GLSurfaceViewRender(this);
        setRenderer(mRender);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    /**
     * 帧数据到来回调，请求Render重绘界面
     * @param surfaceTexture
     */
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }
}
