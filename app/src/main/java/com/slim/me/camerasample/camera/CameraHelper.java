package com.slim.me.camerasample.camera;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CameraHelper {

    private static final String TAG = "CameraHelper";

    public static final int CAMERA_FRONT = 1;
    public static final int CAMERA_BACK = 2;

    // 错误码
    public static final int CODE_OPEN_SUCCESS = 0;  // 成功
    public static final int CODE_OPEN_FAILED = 1;   // 失败
    public static final int CODE_COUNTED_ZERO = 2;  // 没有支持的摄像头
    public static final int CODE_CAMERA_OPENED = 3; // 已经打开了摄像头
    public static final int CODE_CAMERA_GET_PARAM = 4; // 获取CameraParams失败

    private static double ASPECT_TOLERANCE = 0.1f;

    private static CameraHelper mInstance ;
    private Camera mCamera;

    private volatile boolean mIsOpened;  // 已经打开
    private volatile boolean mIsPreviewing; //正在预览

    private int mCurrentCameraId;
    private int mPreviewFormat; // 帧数据格式
    private int mDisplayOrientation;

    private CustomSize mPreviewSize;
    private CustomSize mPictureSize;

    private byte[] USER_BUFFER_1;
    private byte[] USER_BUFFER_2;

    private CameraHelper() {}

    public static CameraHelper getInstance() {
        if(mInstance == null) {
            synchronized (CameraHelper.class) {
                if(mInstance == null) {
                    mInstance = new CameraHelper();
                }
            }
        }
        return mInstance;
    }

    public int openCamera(int id) {
        if(mIsOpened) {
            return CODE_CAMERA_OPENED;
        }
        try {
            mCurrentCameraId = getCameraId(id);
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD) {
                mCamera = Camera.open(mCurrentCameraId);
            }else {
                mCamera = Camera.open();
            }
            if (mCamera == null) {
                return CODE_OPEN_FAILED;
            }
            if (!CameraAbility.getInstance().bindCamera(mCamera)) {
                return CODE_CAMERA_GET_PARAM;
            }
            mIsOpened = true;
            return CODE_OPEN_SUCCESS;
        }catch (Exception e) {
            e.printStackTrace();
            mCamera = null;
            return CODE_OPEN_FAILED;
        }
    }

    public boolean setSurfaceHolder(SurfaceHolder holder) {
        if(null == mCamera || holder == null) {
            return false;
        }
        // 正在预览，不允许更换SurfaceHolder
        if(mIsPreviewing) {
            Log.i(TAG, "is previewing, discard setSurfaceHolder.");
            return false;
        }
        try {
            mCamera.setPreviewDisplay(holder);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setSurfaceTexture(SurfaceTexture texture) {
        if(null == mCamera) {
            Log.e(TAG, "Camera is null, setSurfaceTexture failed.");
            return;
        }
        // 正在预览，不允许更换SurfaceTexture
        if(mIsPreviewing) {
            Log.e(TAG, "previewing, setSurfaceTexture failed.");
            return;
        }
        try {
            mCamera.setPreviewTexture(texture);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setPreViewCallback(Camera.PreviewCallback callback, boolean useBuffer) {
        if(mCamera == null) {
            Log.e(TAG, "Camera is null, setPreViewCallback failed.");
            return;
        }
        try {
            if(useBuffer) {
                makeSureBuffer();
                mCamera.addCallbackBuffer(USER_BUFFER_1);
                mCamera.addCallbackBuffer(USER_BUFFER_2);
                mCamera.setPreviewCallbackWithBuffer(callback);
            }else {
                mCamera.setPreviewCallback(callback);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean addUserBufferRecycle(byte[] buffer) {
        if(mCamera == null) {
            return false;
        }
        try {
            mCamera.addCallbackBuffer(buffer);
            return true;
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void startPreview() {
        if(mCamera == null) {
            Log.e(TAG, "Camera is null. startPreview failed.");
            return;
        }
        if(mIsPreviewing) {
            Log.e(TAG, "previewing. startPreview failed.");
            return;
        }
        try {
            mCamera.startPreview();
            mIsPreviewing = true;
        }catch (Exception e) {
            e.printStackTrace();
            mIsPreviewing = false;
        }
    }

    public void stopPreview() {
        if(mCamera == null) {
            Log.e(TAG, "Camera is null. stopPreview failed.");
            return;
        }
        if(!mIsPreviewing) {
            Log.e(TAG, "is not previewing. stopPreview failed");
            return;
        }
        try {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.setPreviewDisplay(null);
            mCamera.setPreviewTexture(null);
            mIsPreviewing = false;
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setCameraParameters(Camera.Parameters params) {
        if(mCamera == null) {
            Log.e(TAG, "Camera is null, setCameraParameters failed.");
            return;
        }
        if (params == null) {
            Log.e(TAG, "params is null, setCameraParameters failed.");
            return;
        }
        try {
            mCamera.setParameters(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    public Camera.Parameters getCameraParameters() {
        if(mCamera == null) {
            Log.e(TAG, "Camera is null, getCameraParameters failed.");
            return null;
        }
        try {
            return mCamera.getParameters();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setDisplayOrientation(int degrees) {
        if(mCamera == null) {
            Log.e(TAG, "Camera is null, setDisplayOrientation failed.");
            return;
        }
        try {
            mCamera.setDisplayOrientation(degrees);
            mDisplayOrientation = degrees;
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setPreviewFormat(int format) {
        if(mCamera == null) {
            Log.e(TAG, "Camera is null, setPreviewFormat failed.");
            return;
        }

        if(!CameraAbility.getInstance().isSupportPreviewFormat(format)) {
            Log.e(TAG, "format:" + format + " is not supportPreviewFormat.");
            return;
        }
        try {
            Camera.Parameters parameters = getCameraParameters();
            if(parameters != null) {
                parameters.setPreviewFormat(format);
                setCameraParameters(parameters);
                mPreviewFormat = format;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setFocusMode(String mode) {
        if(mCamera == null) {
            Log.e(TAG, "Camera is null, setFocusMode failed.");
            return;
        }
        try {
            Camera.Parameters params = getCameraParameters();
            if (params != null) {
                List<String> supportModes = params.getSupportedFocusModes();
                if (supportModes.contains(mode)) {
                    params.setFocusMode(mode);
                    mCamera.setParameters(params);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void focus(float x, float y, int size, int surfaceWidth, int surfaceHeight) {
        if (mCamera == null) {
            Log.e(TAG, "Camera is null, focus failed.");
        }
        final Camera.Parameters params = getCameraParameters();
        if (params == null) {
            return;
        }
        int centerX = (int) (x / surfaceWidth * 2000 - 1000);
        int centerY = (int) (y / surfaceHeight * 2000 - 1000);
        int left = clamp(centerX - (size / 2), -1000, 1000);
        int top = clamp(centerY - (size / 2), -1000, 1000);
        int right = clamp(left + size, -1000, 1000);
        int bottom = clamp(top + size, -1000, 1000);
        Rect rect = new Rect(left, top, right, bottom);

        mCamera.cancelAutoFocus();
        if (params.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> focusAreas = new ArrayList<>();
            focusAreas.add(new Camera.Area(rect, 800));
            params.setFocusAreas(focusAreas);
        }
        final String currentFocusMode = params.getFocusMode();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
        mCamera.setParameters(params);
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                params.setFocusMode(currentFocusMode);
                mCamera.setParameters(params);
            }
        });
    }

    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    public CustomSize getMatchedPreviewSize(int width, int height) {
        // 先获取支持的Size
        List<Camera.Size> supportPreviewSizes = CameraAbility.getInstance().getPreviewSizes();

        if(supportPreviewSizes == null || supportPreviewSizes.isEmpty()) {
            return null;
        }

        // 先找到和宽高比一致的sizes
        int max = Math.max(width, height);
        int min = Math.min(width, height);
        float ratio = (float) max / min;

        // 和屏幕宽高比一致的PreviewSize
        List<Camera.Size> matchedWantedSizeRatioPreviewList = getMatchedRatioSize(supportPreviewSizes, ratio);

        if (matchedWantedSizeRatioPreviewList != null && !matchedWantedSizeRatioPreviewList.isEmpty()) {
            Camera.Size previewSize = getBestMatchedSize(matchedWantedSizeRatioPreviewList, width, height, 1.5f, 0.2f);
            if (previewSize != null) {
                return new CustomSize(previewSize.width, previewSize.height);
            }
        }

        return null;
    }

    public CustomSize getMatchedPictureSize(int width, int height) {
        // 先获取支持的Size
        List<Camera.Size> supportPictureSizes = CameraAbility.getInstance().getPictureSizes();

        if(supportPictureSizes == null || supportPictureSizes.isEmpty()) {
            return null;
        }

        // 先找到和宽高比一致的sizes
        int max = Math.max(width, height);
        int min = Math.min(width, height);
        float ratio = (float) max / min;

        // 和屏幕宽高比一致的PictureSize
        List<Camera.Size> matchedWantedSizeRatioPictureList = getMatchedRatioSize(supportPictureSizes, ratio);

        if (matchedWantedSizeRatioPictureList != null && !matchedWantedSizeRatioPictureList.isEmpty()) {
            Camera.Size pictureSize = getBestMatchedSize(matchedWantedSizeRatioPictureList, width, height, 0.3f, 0.2f);
            if (pictureSize != null) {
                return new CustomSize(pictureSize.width, pictureSize.height);
            }
        }

        return null;
    }

    /**
     * 设置预览Size
     */
    public void setPreviewSize(CustomSize size) {
        if(mCamera == null) {
            Log.e(TAG, "Camera is null, setPreviewSize failed.");
            return;
        }
        Camera.Parameters params = getCameraParameters();
        if(params == null) {
            Log.e(TAG, "Parameters is null, setPreviewSize failed.");
            return;
        }

        try {
            params.setPreviewSize(size.width, size.height);
            mCamera.setParameters(params);
            mPreviewSize = size;
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置拍照Size
     */
    public void setPictureSize(CustomSize size) {
        if(mCamera == null) {
            Log.e(TAG, "Camera is null, setPictureSize failed.");
            return;
        }
        Camera.Parameters params = getCameraParameters();
        if(params == null) {
            Log.e(TAG, "Parameters is null, setPictureSize failed.");
            return;
        }

        try {
            params.setPictureSize(size.width, size.height);
            mCamera.setParameters(params);
            mPictureSize = size;
        }catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Nullable
    public Camera getCamera() {
        return mCamera;
    }

    public void releaseCamera() {
        Log.d(TAG, "releaseCamera");

        if(!mIsOpened) {
            return;
        }
        CameraAbility.getInstance().reset();
        if (mCamera == null) {
            return;
        }

        try {
            mCamera.release();
            mCamera = null;
            mIsOpened = false;
            mPreviewSize = null;
            mPictureSize = null;
            mDisplayOrientation = 0;
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isOpened() {
        return mIsOpened;
    }

    public boolean isPreviewing() {
        return isOpened() && mIsPreviewing;
    }

    private int getCameraId(int id) {
        int cameraId = -1;
        switch (id) {
            case CAMERA_FRONT:
                if(CameraAbility.hasFrontCamera()) {
                    cameraId = CameraAbility.getFrontCameraId();
                }
                break;
            case CAMERA_BACK:
                if(CameraAbility.hasBackCamera()) {
                    cameraId = CameraAbility.getBackCameraId();
                }
                break;
            default:
                cameraId = CameraAbility.getBackCameraId();
                break;
        }
        return cameraId;
    }

    private void makeSureBuffer() {
        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();

        // 1*Y + 1/4 * U + 1/4 * V = 3/2
        int bufferSize = previewSize.width * previewSize.height * 3 / 2;

        if(USER_BUFFER_1 == null || USER_BUFFER_1.length != bufferSize) {
            USER_BUFFER_1 = new byte[bufferSize];
        }
        if(USER_BUFFER_2 == null || USER_BUFFER_2.length != bufferSize) {
            USER_BUFFER_2 = new byte[bufferSize];
        }
    }

    /**
     * 从sizeList里，找出和width/height最接近的size
     * 需要确保bigEdge > smallEdge
     * 这里不会再校验ratio
     * @param sizeList
     * @param width
     * @param height
     * @param maxDiffer 比目标尺寸大的Size中，最大不能大过maxDiffer
     * @param minDiffer 比目标尺寸小的Size中，最小不能小过minDiffer
     * @return
     */
    private Camera.Size getBestMatchedSize(List<Camera.Size> sizeList, int width, int height, float maxDiffer, float minDiffer) {
        int targetPixel = width * height;
        int bigMinDifferPixel = Integer.MAX_VALUE; // 比目标尺寸大的Size里，最小的differ
        int smallMinDifferPixel = Integer.MIN_VALUE; // 比目标尺寸小的Size里，最小的differ(负数)
        Camera.Size minBigDifferSize = null; // 比目标尺寸大的size里，最接近的size
        Camera.Size minSmallDifferSize = null; // 比目标尺寸小的size里，最接近的size

        for(Camera.Size size : sizeList) {
            int differPixel = size.width * size.height - targetPixel;
            if(differPixel >= 0) {
                float pixelDifferWithTarget = (float)differPixel / targetPixel;
                if(pixelDifferWithTarget > maxDiffer) {
                    // 过大，不考虑
                }else {
                    if(differPixel < bigMinDifferPixel) {
                        bigMinDifferPixel = differPixel;
                        minBigDifferSize = size;
                    }
                }
                continue;
            }

            if(differPixel < 0){
                float pixelDifferWithTarget = (float)-differPixel / targetPixel;
                if(pixelDifferWithTarget < minDiffer) {
                    // 过小，不考虑
                }else {
                    if(differPixel > smallMinDifferPixel) {
                        smallMinDifferPixel = differPixel;
                        minSmallDifferSize = size;
                    }
                }
            }
        }

        // 优先返回大的Size，保证质量
        if(minBigDifferSize != null) {
            return minBigDifferSize;
        }
        if(minSmallDifferSize != null) {
            return minSmallDifferSize;
        }
        return null;
    }

    /**
     * 从一组List中选择出和想要的ratio从相差ASPECT_TOLERANCE以内的Sizes
     * @param sizes
     * @param wantedRatio
     * @return
     */
    private List<Camera.Size> getMatchedRatioSize(List<Camera.Size> sizes, float wantedRatio) {
        int bigEdge, smallEdge;
        float ratio;
        List<Camera.Size> matchedSizes = new ArrayList<>();
        for(Camera.Size size : sizes) {
            bigEdge = Math.max(size.width, size.height);
            smallEdge = Math.min(size.width, size.height);
            ratio = (float)bigEdge / smallEdge;
            if(Math.abs(ratio - wantedRatio) < ASPECT_TOLERANCE) {
                matchedSizes.add(size);
            }
        }
        return matchedSizes;
    }


    public static class CustomSize {
        // 摄像头是横屏的，所以width > height;
        public int width;
        public int height;
        /**
         * w:h <br>
         * 摄像头是横屏的，所以width > height，故该值>1
         */
        private double scaleWH;

        public CustomSize() {
            // nothing
        }

        public CustomSize(int width, int height) {
            super();
            this.width = width;
            this.height = height;
            this.scaleWH = (double) width / (double) height;
        }

        public double getScaleWH() {
            if (scaleWH == 0) {
                scaleWH = (double) width / (double) height;
            }

            return scaleWH;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof CustomSize)) return false;
            CustomSize temp = (CustomSize) obj;
            return (width == temp.width && height == temp.height);
        }
    }

}
