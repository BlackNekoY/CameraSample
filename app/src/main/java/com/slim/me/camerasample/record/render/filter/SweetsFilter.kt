package com.slim.me.camerasample.record.render.filter

import android.graphics.BitmapFactory
import android.opengl.GLES30
import com.slim.me.camerasample.R
import com.slim.me.camerasample.app.BaseApplication
import com.slim.me.camerasample.util.OpenGLUtils
import java.nio.ByteBuffer

class SweetsFilter : GPUImageFilter() {

    private var mToneCurveTexture = -1
    private var mMaskTexture = -1

    override fun getFragmentShader(): String {
        return OpenGLUtils.readShaderFromRawResource(R.raw.sweets)
    }

    override fun onInitialized() {
        super.onInitialized()
        mToneCurveTexture = OpenGLUtils.createTexture(GLES30.GL_TEXTURE_2D)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mToneCurveTexture)
        val arrayOfByte = ByteArray(1024)
        val arrayOfInt = intArrayOf(0, 1, 2, 2, 3, 4, 5, 6, 6, 7, 8, 9, 10, 10, 11, 12, 13, 14, 14, 15, 16, 17, 18, 19, 19, 20, 21, 22, 23, 24, 24, 25, 26, 27, 28, 29, 30, 30, 31, 32, 33, 34, 35, 36, 37, 38, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 71, 72, 73, 74, 75, 76, 77, 79, 80, 81, 82, 83, 84, 86, 87, 88, 89, 90, 92, 93, 94, 95, 96, 98, 99, 100, 101, 103, 104, 105, 106, 108, 109, 110, 111, 113, 114, 115, 116, 118, 119, 120, 121, 123, 124, 125, 126, 128, 129, 130, 132, 133, 134, 135, 137, 138, 139, 140, 142, 143, 144, 145, 147, 148, 149, 150, 152, 153, 154, 155, 157, 158, 159, 160, 161, 163, 164, 165, 166, 167, 169, 170, 171, 172, 173, 174, 176, 177, 178, 179, 180, 181, 182, 183, 184, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 209, 210, 211, 212, 213, 214, 215, 216, 217, 217, 218, 219, 220, 221, 222, 222, 223, 224, 225, 226, 227, 227, 228, 229, 230, 230, 231, 232, 233, 234, 234, 235, 236, 237, 237, 238, 239, 240, 240, 241, 242, 243, 243, 244, 245, 246, 246, 247, 248, 248, 249, 250, 251, 251, 252, 253, 254, 254, 255)
        for (i in 0..255) {
            arrayOfByte[i * 4] = arrayOfInt[i].toByte()
            arrayOfByte[1 + i * 4] = arrayOfInt[i].toByte()
            arrayOfByte[2 + i * 4] = arrayOfInt[i].toByte()
            arrayOfByte[3 + i * 4] = i.toByte()
        }
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, 256, 1, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, ByteBuffer.wrap(arrayOfByte))
        mMaskTexture = OpenGLUtils.createTexture2D(BitmapFactory.decodeResource(BaseApplication.getIns().resources,R.drawable.rise_mask2))
    }

    override fun onPreDraw(textureId: Int, cameraMatrix: FloatArray?, textureMatrix: FloatArray?) {
        super.onPreDraw(textureId, cameraMatrix, textureMatrix)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mToneCurveTexture)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(program, "curve"), 1)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mMaskTexture)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(program, "samplerMask"), 2)

        GLES30.glUniform1i(GLES30.glGetUniformLocation(program, "lowPerformance"), 1)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(program, "texelWidthOffset"), 1.0f / outputWidth.toFloat())
        GLES30.glUniform1f(GLES30.glGetUniformLocation(program, "texelHeightOffset"), 1.0f / outputHeight.toFloat())
    }
}