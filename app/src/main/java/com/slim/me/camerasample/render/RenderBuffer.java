package com.slim.me.camerasample.render;

import android.opengl.GLES20;

import com.slim.me.camerasample.util.GlUtil;

/**
 * Created by slimxu on 2018/2/9.
 */

public class RenderBuffer {
    private int mFrameBufferId;
    private int mTextureId;
    private int mRenderBufferId;
    private int mActivitUnit;

    private int mWidth;
    private int mHeight;

    public RenderBuffer(int width, int height, int activeUnit) {
        mWidth = width;
        mHeight = height;
        mActivitUnit = activeUnit;

        int[] frameBuffers = new int[1];
        int[] renderBuffers = new int[1];

        GLES20.glActiveTexture(activeUnit);
        mTextureId = GlUtil.createTexture(GLES20.GL_TEXTURE_2D);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mWidth, mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glGenFramebuffers(1, frameBuffers, 0);
        mFrameBufferId = frameBuffers[0];
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferId);

        GLES20.glGenRenderbuffers(1, renderBuffers, 0);
        mRenderBufferId = renderBuffers[0];
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mRenderBufferId);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, mWidth, mHeight);

        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mTextureId,0 );
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, mRenderBufferId);
    }

    public void bind() {
        GLES20.glViewport(0, 0, mWidth, mHeight);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mTextureId,0 );
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, mRenderBufferId);
    }

    public void unbind() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void destroy() {
        int[] bufferIds = new int[1];
        bufferIds[0] = mTextureId;
        GLES20.glDeleteTextures(1, bufferIds, 0);

        bufferIds[0] = mFrameBufferId;
        GLES20.glDeleteFramebuffers(1, bufferIds, 0);

        bufferIds[0] = mRenderBufferId;
        GLES20.glDeleteRenderbuffers(1, bufferIds, 0);
    }

    public int getTextId() {
        return mTextureId;
    }
}
