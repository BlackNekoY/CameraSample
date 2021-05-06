package com.slim.me.camerasample.record.render;

import android.opengl.GLES30;

import com.slim.me.camerasample.record.render.filter.GPUImageFilter;
import com.slim.me.camerasample.record.render.filter.ImageFilterGroup;

import java.util.ArrayList;
import java.util.LinkedList;

public class Texture2DRender {

    /**
     * 默认滤镜，用于拷贝texture，从FboA -> FboB
     */
    private GPUImageFilter mCopyFilter;

    /**
     * 左半部分渲染滤镜
     */
    private GPUImageFilter mLeftFilter;

    /**
     * 又半部分渲染滤镜
     */
    private GPUImageFilter mRightFilter;

    /**
     * 水印，和滤镜链无关的最后一道滤镜
     */
    private GPUImageFilter mWatermarkFilter;

    private final ArrayList<Action> mActions = new ArrayList<>();
    private final Action mDrawTextureAction = new Action() {
        @Override
        public void doAction(int textureId, float[] cameraMatrix, float[] textureMatrix) {
            drawTextureInner(textureId, cameraMatrix, textureMatrix);
        }
    };

    private final Action mDrawWaterMarkAction = new Action() {
        @Override
        public void doAction(int textureId, float[] cameraMatrix, float[] textureMatrix) {
            drawWaterMark(textureId, cameraMatrix, textureMatrix);
        }
    };

    private final LinkedList<Runnable> mPendingGLThreadTask = new LinkedList<>();
    private final LinkedList<FrameBuffer> mCacheFrameBuffers = new LinkedList<>();
    private FrameBuffer mRenderFBO;     // 要将上屏的这一个texture保存起来送去录制，不然会丢
    private FrameBufferFactory mRenderFboFactory;
    private int mWidth, mHeight;

    private float mLeftOffset = 1f;

    public void init() {
        if (mCopyFilter == null) {
            mCopyFilter = new GPUImageFilter();
        }
        mRenderFboFactory = new FrameBufferFactory();
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

    private void checkFilterFrameBuffer(boolean set) {
        if (mLeftFilter != null && mLeftFilter.isInit() && mLeftFilter instanceof ImageFilterGroup) {
            ((ImageFilterGroup) mLeftFilter).setFrameBufferFactory(set ? mRenderFboFactory : null);
        }
        if (mRightFilter != null && mRightFilter.isInit() && mRightFilter instanceof ImageFilterGroup) {
            ((ImageFilterGroup) mRightFilter).setFrameBufferFactory(set ? mRenderFboFactory : null);
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
        checkFilterFrameBuffer(true);

        mRenderFBO.bind();
        // **************
        mActions.clear();
        mActions.add(mDrawTextureAction);
        if (mWatermarkFilter != null) {
            mActions.add(mDrawWaterMarkAction);
        }
        doAllAction(textureId, cameraMatrix, textureMatrix);
        // **************
        mRenderFBO.unbind();

        mCopyFilter.draw(mRenderFBO.getTextureId(), null, null);

        checkFilterFrameBuffer(false);
        repayAllFrameBuffers();
    }

    private void doAllAction(int textureId, float[] cameraMatrix, float[] textureMatrix) {
        int drawTexId = textureId;
        for (int i = 0;i < mActions.size(); i++) {
            Action action = mActions.get(i);
            if (i == mActions.size() - 1) {
                action.doAction(drawTexId, cameraMatrix, textureMatrix);
            } else {
                FrameBuffer fbo = applyFrameBuffer();
                fbo.bind();
                action.doAction(drawTexId, cameraMatrix, textureMatrix);
                fbo.unbind();
                drawTexId = fbo.getTextureId();
            }
        }
    }

    private void drawTextureInner(int textureId, float[] cameraMatrix, float[] textureMatrix) {
        if (mLeftFilter == null && mRightFilter == null) {
            return;
        }
        if (mRightFilter != null) {
            drawTextureScissor(textureId, cameraMatrix, textureMatrix);
        } else {
            drawTextureNormal(textureId, cameraMatrix, textureMatrix);
        }
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

    private void drawTextureNormal(int textureId, float[] cameraMatrix, float[] textureMatrix) {
        GLES30.glViewport(0, 0, mWidth, mHeight);
        GLES30.glDisable(GLES30.GL_SCISSOR_TEST);
        mLeftFilter.draw(textureId, cameraMatrix, textureMatrix);
    }

    private void drawWaterMark(int textureId, float[] cameraMatrix, float[] textureMatrix) {
        if (mWatermarkFilter == null) {
            return;
        }
        mWatermarkFilter.draw(textureId, cameraMatrix, textureMatrix);
    }

    private FrameBuffer applyFrameBuffer() {
        FrameBuffer fbo = mRenderFboFactory.applyFrameBuffer();
        mCacheFrameBuffers.addLast(fbo);
        return fbo;
    }

    private void repayAllFrameBuffers() {
        for (FrameBuffer fbo : mCacheFrameBuffers) {
            mRenderFboFactory.repayFrameBuffer(fbo);
        }
        mCacheFrameBuffers.clear();
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

    public void setWatermarkFilter(final GPUImageFilter filter) {
        synchronized (mPendingGLThreadTask) {
            mPendingGLThreadTask.addLast(new Runnable() {
                @Override
                public void run() {
                    mWatermarkFilter = filter;
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
        mWidth = width;
        mHeight = height;

        mRenderFboFactory.initFrameBuffers(width, height);
        if (mRenderFBO != null) {
            mRenderFBO.release();
            mRenderFBO = null;
        }
        mRenderFBO = new FrameBuffer(width, height);

        GLES30.glViewport(0, 0, width, height);
        onFiltersSizeChanged(width, height);
    }

    public int getTextureId() {
        return mRenderFBO.getTextureId();
    }

    public void release() {
        mRenderFboFactory.deleteFrameBuffers();
        mRenderFBO.release();
        mRenderFBO = null;
        if (mCopyFilter != null) {
            mCopyFilter.destroy();
        }
        if (mLeftFilter != null) {
            mLeftFilter.destroy();
        }
        if (mRightFilter != null) {
            mRightFilter.destroy();
        }
        if (mWatermarkFilter != null) {
            mWatermarkFilter.destroy();
        }
    }

    interface Action {
        void doAction(int textureId, float[] cameraMatrix, float[] textureMatrix);
    }
}
