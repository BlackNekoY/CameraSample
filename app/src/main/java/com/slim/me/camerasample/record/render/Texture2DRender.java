package com.slim.me.camerasample.record.render;


import android.support.annotation.NonNull;

import com.slim.me.camerasample.record.render.filter.BaseFilter;


public class Texture2DRender {

    private BaseFilter mCurrentFilter;

    public void drawTexture(int textureId, float[] textureMatrix) {
        if (mCurrentFilter != null) {
            mCurrentFilter.draw(textureId, textureMatrix);
        }
    }

    public void setFilter(@NonNull BaseFilter filter) {
        mCurrentFilter = filter;
    }
}
