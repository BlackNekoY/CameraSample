package com.slim.me.camerasample.record.render;

import android.graphics.BitmapFactory;
import android.opengl.GLES30;

import com.slim.me.camerasample.R;
import com.slim.me.camerasample.app.BaseApplication;
import com.slim.me.camerasample.record.render.filter.GPUImageFilter;
import com.slim.me.camerasample.record.render.filter.WatermarkFilter;

import java.util.LinkedList;

public class Texture2DRender {

    private GPUImageFilter mLeftFilter;
    private GPUImageFilter mRightFilter;
    private WatermarkFilter mWatermarkFilter;
    private GPUImageFilter mCopyFilter;

    private final LinkedList<Runnable> mPendingGLThreadTask = new LinkedList<>();
    private FrameBuffer mRenderFBO;
    private int mWidth, mHeight;

    private float mLeftOffset = 1f;

    public void init() {
        if (mCopyFilter == null) {
            mCopyFilter = new GPUImageFilter();
        }
        if (mWatermarkFilter == null) {
            mWatermarkFilter = new WatermarkFilter(BitmapFactory.decodeResource(BaseApplication.Companion.getIns().getResources(), R.drawable.awesomeface));
        }
        checkFilterInit();
    }

    private void checkFilterInit() {
        if (mCopyFilter != null && !mCopyFilter.isInit()) {
            mCopyFilter.init();
            mCopyFilter.onOutputSizeChanged(mWidth, mHeight);
        }
        if (mLeftFilter != null && !mLeftFilter.isInit()) {
            mLeftFilter.init();
            mLeftFilter.onOutputSizeChanged(mWidth, mHeight);
        }
        if (mRightFilter != null && !mRightFilter.isInit()) {
            mRightFilter.init();
            mRightFilter.onOutputSizeChanged(mWidth, mHeight);
        }
        if (mWatermarkFilter != null && !mWatermarkFilter.isInit()) {
            mWatermarkFilter.init();
            mWatermarkFilter.onOutputSizeChanged(mWidth, mHeight);
        }
    }

    private void onFiltersSizeChanged(int width, int height) {
        if (mCopyFilter != null) {
            mCopyFilter.onOutputSizeChanged(width, height);
        }
        if (mLeftFilter != null) {
            mLeftFilter.onOutputSizeChanged(width, height);
        }
        if (mRightFilter != null) {
            mRightFilter.onOutputSizeChanged(width, height);
        }
        if (mWatermarkFilter != null) {
            mWatermarkFilter.onOutputSizeChanged(width, height);
        }
    }

    public void drawTexture(int textureId, float[] cameraMatrix, float[] textureMatrix) {
        doAllPendingTask();
        if (mCopyFilter == null || mLeftFilter == null) {
            return;
        }
        checkFilterInit();

        mRenderFBO.bind();
        if (mRightFilter != null) {
            drawTextureScissor(textureId, cameraMatrix, textureMatrix);
        } else {
            drawTextureInner(textureId, cameraMatrix, textureMatrix);
        }
        mRenderFBO.unbind();
        mCopyFilter.draw(mRenderFBO.getTextureId(), null, null);
    }

    private void drawTextureScissor(int textureId, float[] cameraMatrix, float[] textureMatrix) {
        GLES30.glViewport(0, 0, mWidth, mHeight);
        GLES30.glEnable(GLES30.GL_SCISSOR_TEST);
        GLES30.glScissor(0, 0, (int) (mWidth * mLeftOffset), mHeight);
        mLeftFilter.draw(textureId, cameraMatrix, textureMatrix);
        GLES30.glDisable(GLES30.GL_SCISSOR_TEST);
        GLES30.glViewport(0, 0, mWidth, mHeight);
        GLES30.glEnable(GLES30.GL_SCISSOR_TEST);
        GLES30.glScissor((int) (mWidth * mLeftOffset), 0, (int) (mWidth * (1 - mLeftOffset)), mHeight);
        mRightFilter.draw(textureId, cameraMatrix, textureMatrix);
        GLES30.glDisable(GLES30.GL_SCISSOR_TEST);
        GLES30.glViewport(0, 0, mWidth, mHeight);
    }

    private void drawTextureInner(int textureId, float[] cameraMatrix, float[] textureMatrix) {
        GLES30.glViewport(0, 0, mWidth, mHeight);
        GLES30.glDisable(GLES30.GL_SCISSOR_TEST);
        mLeftFilter.draw(textureId, cameraMatrix, textureMatrix);
    }

    private void drawWaterMark(int textureId, float[] cameraMatrix, float[] textureMatrix) {
        if (mWatermarkFilter != null) {
            mWatermarkFilter.draw(textureId, cameraMatrix, textureMatrix);
        }
    }

    private void doAllPendingTask() {
        while (!mPendingGLThreadTask.isEmpty()) {
            mPendingGLThreadTask.removeFirst().run();
        }
    }

    public void setFilter(final GPUImageFilter filter) {
        synchronized (mPendingGLThreadTask) {
            mPendingGLThreadTask.addLast(new Runnable() {
                @Override
                public void run() {
                    mLeftFilter = filter;
                }
            });
        }
    }

    public void setLeftFilter(final GPUImageFilter filter) {
        synchronized (mPendingGLThreadTask) {
            mPendingGLThreadTask.addLast(new Runnable() {
                @Override
                public void run() {
                    mLeftFilter = filter;
                }
            });
        }
    }

    public void setRightFilter(final GPUImageFilter filter) {
        synchronized (mPendingGLThreadTask) {
            mPendingGLThreadTask.addLast(new Runnable() {
                @Override
                public void run() {
                    mRightFilter = filter;
                }
            });
        }
    }

    public void setScrollX(final float x) {
        synchronized (mPendingGLThreadTask) {
            mPendingGLThreadTask.addLast(new Runnable() {
                @Override
                public void run() {
                    mLeftOffset = x;
                }
            });
        }
    }

    public void onSizeChanged(final int width, final int height) {
        GLES30.glViewport(0, 0, width, height);
        mWidth = width;
        mHeight = height;
        mRenderFBO = new FrameBuffer(width, height);
        onFiltersSizeChanged(width, height);
    }

    public int getTextureId() {
        return mRenderFBO.getTextureId();
    }
}
