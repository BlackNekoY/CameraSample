package com.slim.me.camerasample.surfaceview_with_egl;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.slim.me.camerasample.R;

/**
 * 使用SurfaceView + 自配EGL环境example
 * Created by slimxu on 2018/1/3.
 */

public class EGLSurfaceActivity extends AppCompatActivity {
    private SurfaceView mSurfaceView;
    private GLRender mGLRender;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_egl_surface);

        mSurfaceView = (SurfaceView) findViewById(R.id.sv_main_demo);
        mGLRender = new GLRender();
        mGLRender.start();

        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mGLRender.render(holder.getSurface(), width, height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        mGLRender.release();
        mGLRender = null;
        super.onDestroy();
    }

    private class A extends SurfaceView {
        public A(Context context) {
            super(context);
        }
    }

}
