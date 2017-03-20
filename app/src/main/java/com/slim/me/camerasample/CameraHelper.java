package com.slim.me.camerasample;

import android.hardware.Camera;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;

public class CameraHelper {

    private static final String TAG = "CameraHelper";

    private static CameraHelper sInstance = new CameraHelper();

    public static CameraHelper getInstance() {
        return sInstance;
    }

    private CameraHelper() {
    }

    private Camera mCamera;

    public boolean openCamera() {
        try {
            mCamera = Camera.open();
            return mCamera != null;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return false;
    }

    @Nullable
    public Camera getCamera() {
        return mCamera;
    }

    public void releaseCamera() {
        if (mCamera == null) {
            return;
        }
        Log.d(TAG, "releaseCamera");
        mCamera.release();
        mCamera = null;
    }

    public static Camera.Size getFitPreviewSize(Camera camera, int surfaceWidth, int surfaceHeight) {
        List<Camera.Size> supportPreviewSizes = camera.getParameters().getSupportedPreviewSizes();
        return getOptimalSize(supportPreviewSizes, surfaceWidth, surfaceHeight);
    }

    public static Camera.Size getFitPictureSize(Camera camera, int surfaceWidth, int surfaceHeight) {
        List<Camera.Size> supportPictureSizes = camera.getParameters().getSupportedPictureSizes();
        return getOptimalSize(supportPictureSizes, surfaceWidth, surfaceHeight);
    }

    /**
     * Calculate the optimal size of camera preview
     *
     * @param sizes
     * @param w
     * @param h
     * @return
     */
    public static Camera.Size getOptimalSize(List<Camera.Size> sizes, int w, int h) {

        final double ASPECT_TOLERANCE = 0.2;
        double targetRatio = (double) w / h;
        if (sizes == null)
            return null;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }


        return optimalSize;
    }

}
