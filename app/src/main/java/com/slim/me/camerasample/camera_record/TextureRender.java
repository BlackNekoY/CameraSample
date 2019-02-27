package com.slim.me.camerasample.camera_record;

import android.opengl.GLES11Ext;


public class TextureRender {
    private Texture2DRender m2DRedner;
    private TextureOESRender mOESRender;

    /**
     * 必须在GL线程调用
     */
    public TextureRender() {
        m2DRedner = new Texture2DRender();
        mOESRender = new TextureOESRender();
        m2DRedner.init();
        mOESRender.init();
    }

    public void drawTexture(int textureType, int textureId, float[] textureMatrix) {
        if (textureType == GLES11Ext.GL_TEXTURE_EXTERNAL_OES) {
            mOESRender.drawTexture(textureId, textureMatrix);
        } else {
            m2DRedner.drawTexture(textureId, textureMatrix);
        }
    }
}
