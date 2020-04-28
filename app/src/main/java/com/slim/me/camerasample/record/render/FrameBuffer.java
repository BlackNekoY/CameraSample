package com.slim.me.camerasample.record.render;


import android.opengl.GLES30;

import com.slim.me.camerasample.util.OpenGLUtils;

public class FrameBuffer {

    /**
     * 离屏FBO的纹理ID
     */
    private int mTextureId;
    /**
     * FBO
     */
    private int mFbo;
    /**
     * rbo
     */
    private int mRbo;

    private int[] mPreviousFbo = new int[]{0};

    private int mWidth;
    private int mHeight;

    public FrameBuffer(int width, int height) {
        mWidth = width;
        mHeight = height;

        bind();
        // 创建FBO
        int[] array = new int[1];
        GLES30.glGenFramebuffers(1, array, 0);
        mFbo = array[0];
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFbo);

        // 创建纹理ID
        mTextureId = OpenGLUtils.createTexture(GLES30.GL_TEXTURE_2D);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureId);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, mWidth, mHeight, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);

        // 创建渲染缓冲RBO
        GLES30.glGenRenderbuffers(1, array, 0);
        mRbo = array[0];
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, mRbo);
        GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT16, mWidth, mHeight);
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, 0);

        // 绑定
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, mTextureId,0 );
        GLES30.glFramebufferRenderbuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_RENDERBUFFER, mRbo);
        
        unbind();
    }

    /**
     * 将FBO绑定到当前Surface上
     */
    public void bind() {
        GLES30.glGetIntegerv(GLES30.GL_FRAMEBUFFER_BINDING, mPreviousFbo, 0);

        GLES30.glViewport(0, 0, mWidth, mHeight);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFbo);
    }

    public void unbind() {
        if (mPreviousFbo[0] < 0) {
            mPreviousFbo[0] = 0;
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mPreviousFbo[0]);
    }

    public int getTextureId() {
        return mTextureId;
    }

    public int getFbo() {
        return mFbo;
    }

    public int getRbo() {
        return mRbo;
    }

    public void release() {
        int[] ids = new int[]{mTextureId, mRbo, mFbo};
        GLES30.glDeleteTextures(1, ids, 0);
        GLES30.glDeleteRenderbuffers(1, ids, 1);
        GLES30.glDeleteFramebuffers(1, ids, 2);
        mTextureId = mRbo = mFbo = -1;
        mPreviousFbo[0] = -1;
    }

}
