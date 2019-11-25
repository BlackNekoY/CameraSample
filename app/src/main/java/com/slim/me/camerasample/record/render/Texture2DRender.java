package com.slim.me.camerasample.record.render;


import android.opengl.GLES30;

import com.slim.me.camerasample.record.render.filter.BaseFilter;
import com.slim.me.camerasample.record.render.filter.BlankFilter;
import com.slim.me.camerasample.record.render.filter.OESFilter;

import java.util.ArrayList;
import java.util.List;


public class Texture2DRender {

    private List<BaseFilter> mFilters = new ArrayList<>();
    private BaseFilter mCopyFilter;
    private FrameBuffer[] mFrameBuffers = new FrameBuffer[3];
    private int mCurrentTextureId;
    private int mSurfaceWidth;
    private int mSurfaceHeight;

    public Texture2DRender() {
        mCopyFilter = new BlankFilter();
        mCopyFilter.init();
    }

    public void drawTexture(int textureId, float[] cameraMatrix, float[] textureMatrix) {
        mCurrentTextureId = textureId;
        if (mFilters.isEmpty()) {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
            mCopyFilter.draw(textureId, cameraMatrix, textureMatrix);
            return;
        }

        for (int i = 0;i < mFilters.size();i++) {
            BaseFilter filter = mFilters.get(i);
            FrameBuffer fbo = mFrameBuffers[i % 3];
            fbo.bind();
            filter.draw(mCurrentTextureId, cameraMatrix, textureMatrix);
            fbo.unbind();
            mCurrentTextureId = fbo.getTextureId();
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        mCopyFilter.draw(mCurrentTextureId, cameraMatrix, textureMatrix);
    }

    public void setFilters(List<BaseFilter> filters) {
        mFilters.clear();
        mFilters.addAll(filters);
    }

    public void setFilter(BaseFilter filter) {
        mFilters.clear();
        mFilters.add(filter);
    }

    /**
     * call on GL thread
     */
    public void onSizeChanged(final int width, final int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        deleteFrameBuffers();
        for (int i = 0; i < 3 ; i++) {
            mFrameBuffers[i] = new FrameBuffer(width, height);
        }
    }

    public int getTextureId() {
        return mCurrentTextureId;
    }

    private void deleteFrameBuffers() {
        for (FrameBuffer fbo : mFrameBuffers) {
            if (fbo != null) {
                final int[] ids = new int[]{fbo.getTextureId(), fbo.getRbo(), fbo.getFbo()};
                GLES30.glDeleteTextures(1, ids, 0);
                GLES30.glDeleteRenderbuffers(1, ids, 1);
                GLES30.glDeleteFramebuffers(1, ids, 2);
            }
        }
        mFrameBuffers = new FrameBuffer[3];
    }
}
