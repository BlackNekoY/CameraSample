package com.slim.me.camerasample.record.render;

import android.opengl.GLES30;

import com.slim.me.camerasample.record.render.filter.GPUImageFilter;
import com.slim.me.camerasample.record.render.filter.ImageFilterGroup;

import java.util.LinkedList;

public class Texture2DRender {

    private GPUImageFilter mLeftFilter;
    private GPUImageFilter mRightFilter;
    private float mLeftOffset = 1f;

    private GPUImageFilter mCopyFilter;
    private FrameBuffer mRenderFBO;
    private int mWidth, mHeight;

    private final LinkedList<Runnable> mPendingGLThreadTask = new LinkedList<>();

    public void init() {
        if (mCopyFilter == null) {
            mCopyFilter = new GPUImageFilter();
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
    }

    public void drawTexture(int textureId, float[] cameraMatrix, float[] textureMatrix) {
        doAllPendingTask();
        if (mCopyFilter == null || mLeftFilter == null) {
            return;
        }
        checkFilterInit();
        // 如果是滤镜链，则滤镜链有自己的一套FBO，需要重新绑定渲染FBO
        if (mLeftFilter instanceof ImageFilterGroup) {
            ((ImageFilterGroup) mLeftFilter).setRenderFrameBuffer(mRenderFBO);
        }
        if (mRightFilter instanceof ImageFilterGroup) {
            ((ImageFilterGroup) mRightFilter).setRenderFrameBuffer(mRenderFBO);
        }

        if (mRightFilter != null) {
            drawTextureScissor(textureId, cameraMatrix, textureMatrix);
        } else {
            drawTextureInner(textureId, cameraMatrix, textureMatrix);
        }
        mCopyFilter.draw(mRenderFBO.getTextureId(), null, null);
    }

    private void drawTextureScissor(int textureId, float[] cameraMatrix, float[] textureMatrix) {
        mRenderFBO.bind();
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
        mRenderFBO.unbind();
    }

    private void drawTextureInner(int textureId, float[] cameraMatrix, float[] textureMatrix) {
        mRenderFBO.bind();
        GLES30.glViewport(0, 0, mWidth, mHeight);
        GLES30.glDisable(GLES30.GL_SCISSOR_TEST);
        mLeftFilter.draw(textureId, cameraMatrix, textureMatrix);
        mRenderFBO.unbind();
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

        if (mCopyFilter != null) {
            mCopyFilter.onOutputSizeChanged(width, height);
        }
        if (mLeftFilter != null) {
            mLeftFilter.onOutputSizeChanged(width, height);
        }
        if (mRightFilter != null) {
            mRightFilter.onOutputSizeChanged(width, height);
        }
    }

    public int getTextureId() {
        return mRenderFBO.getTextureId();
    }
}
