package com.slim.me.camerasample.record.render;

import com.slim.me.camerasample.record.render.filter.GPUImageFilter;
import com.slim.me.camerasample.record.render.filter.ImageFilterGroup;

public class Texture2DRender {

    private GPUImageFilter mRenderFilter;
    private GPUImageFilter mCopyFilter;
    private FrameBuffer mRenderFBO;

    public Texture2DRender() {
        mCopyFilter = new GPUImageFilter();
    }

    public void init() {
        mCopyFilter.init();
        mRenderFilter.init();
    }

    public void drawTexture(int textureId, float[] cameraMatrix, float[] textureMatrix) {
        // 如果是滤镜链，则滤镜链有自己的一套FBO，需要重新绑定渲染FBO
        if (mRenderFilter instanceof ImageFilterGroup) {
            ((ImageFilterGroup) mRenderFilter).setRenderFrameBuffer(mRenderFBO);
        }

        mRenderFBO.bind();
        mRenderFilter.draw(textureId, cameraMatrix, textureMatrix);
        mRenderFBO.unbind();
        mCopyFilter.draw(mRenderFBO.getTextureId(), null, null);
    }

    public void setFilter(GPUImageFilter filter) {
        mRenderFilter = filter;
    }

    public void onSizeChanged(final int width, final int height) {
        mRenderFBO = new FrameBuffer(width, height);
        mRenderFilter.onOutputSizeChanged(width, height);
    }

    public int getTextureId() {
        return mRenderFBO.getTextureId();
    }
}
