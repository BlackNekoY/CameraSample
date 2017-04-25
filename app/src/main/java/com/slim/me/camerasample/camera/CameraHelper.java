package com.slim.me.camerasample.camera;

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

    private static double ASPECT_TOLERANCE = 0.01f;

    private static CameraHelper mInstance ;
    private Camera mCamera;

    private boolean mIsOpened;  // 已经打开
    private boolean mIsPreviewing; //正在预览

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
        int errCode = CODE_OPEN_SUCCESS;
        try {
            mCurrentCameraId = getCameraId(id);
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD) {
                mCamera = Camera.open(mCurrentCameraId);
            }else {
                mCamera = Camera.open();
            }
            errCode = CODE_OPEN_SUCCESS;
        }catch (Exception e) {
            e.printStackTrace();
            errCode = CODE_OPEN_FAILED;
            mCamera = null;
        }

        // 打开失败，直接返回错误码
        if(null == mCamera) {
            return errCode;
        }

        if(!CameraAbility.getInstance().bindCamera(mCamera)) {
            return CODE_CAMERA_GET_PARAM;
        }

        // 打开成功
        mIsOpened = true;
        return CODE_OPEN_SUCCESS;
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

    public boolean setSurfaceTexture(SurfaceTexture texture) {
        if(null == mCamera || texture == null) {
            return false;
        }
        // 正在预览，不允许更换SurfaceTexture
        if(mIsPreviewing) {
            Log.i(TAG, "is previewing, refuse setSurfaceTexture.");
            return false;
        }

        try {
            mCamera.setPreviewTexture(texture);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean setPreViewCallback(Camera.PreviewCallback callback, boolean useBuffer) {
        if(mCamera == null) {
            return false;
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
            return true;
        }catch (Exception e) {
            e.printStackTrace();
        }

        return false;
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


    public boolean startPreview() {
        if(null == mCamera) {
            Log.e(TAG, "Camera is null. refuse startPreview.");
            return false;
        }
        if(mIsPreviewing) {
            Log.e(TAG, "is previewing. refuse startPreview.");
            return false;
        }

        try {
            mCamera.startPreview();
            mIsPreviewing = true;
            return true;
        }catch (Exception e) {
            e.printStackTrace();
            mIsPreviewing = false;
        }
        return false;
    }

    /**
     * stopPreview，will clear PreviewDisplay & PreviewCallback
     */
    public boolean stopPreview() {
        if(mCamera == null) {
            Log.e(TAG, "Camera is null. refuse stopPreview");
            return false;
        }
        if(!mIsPreviewing) {
            Log.e(TAG, "is not previewing. refuse stopPreview");
            return false;
        }

        try {
            mCamera.stopPreview();
            /**
             * You have to unset preview callback before camera.release(), after camera.stopPreview()
             * otherwise it might throw RuntimeException : use camera after called camera.release()
             */
            mCamera.setPreviewCallback(null);
            mCamera.setPreviewDisplay(null);
            mCamera.setPreviewTexture(null);
            mIsPreviewing = false;
            return true;
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean setCameraParameters(Camera.Parameters params) {
        if(mCamera == null) {
            Log.e(TAG, "Camera is null, setCameraParameters failed.");
            return false;
        }
        if (null == params) {
            return false;
        }
        try {
            mCamera.setParameters(params);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Nullable
    public Camera.Parameters getCameraParameters() {
        if(mCamera == null) {
            Log.e(TAG, "Camera is null, getCameraParameters failed.");
            return null;
        }
        Camera.Parameters params = null;
        try {
            params = mCamera.getParameters();
        } catch (Exception e) {
            e.printStackTrace();
            params = null;
        }
        return params;
    }

    public boolean setDisplayOrientation(int degrees) {
        if(mCamera == null) {
            Log.e(TAG, "Camera is null, setDisplayOrientation failed.");
            return false;
        }

        try {
            mCamera.setDisplayOrientation(degrees);
            mDisplayOrientation = degrees;
            return true;
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean setPreviewFormat(int format) {
        if(mCamera == null) {
            Log.e(TAG, "Camera is null, setImageFormat failed.");
            return false;
        }

        if(!CameraAbility.getInstance().isSupportPreviewFormat(format)) {
            Log.e(TAG, "format:" + format + " is not supportPreviewFormat.");
            return false;
        }

        try {
            Camera.Parameters parameters = getCameraParameters();
            if(parameters != null) {
                parameters.setPreviewFormat(format);
                boolean result = setCameraParameters(parameters);
                if(result) {
                    mPreviewFormat = format;
                    return true;
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * 通过想要的宽高和屏幕宽高，得到最合适的pictureSize和previewSize，只有二者都存在才会返回值，其他返回null
     * @param wantedWidth
     * @param wantedHeight
     * @param screenWidth
     * @param screenHeight
     * @return CustomSize[0]是pictureSize，CustomSize[1]是previewSize
     */
    public CustomSize[] getMatchedPreviewPictureSize(int wantedWidth, int wantedHeight, int screenWidth, int screenHeight) {
        // 先获取支持的Size
        List<Camera.Size> supportPictureSizes = CameraAbility.getInstance().getPictureSizes();
        List<Camera.Size> supportPreviewSizes = CameraAbility.getInstance().getPreviewSizes();

        if(supportPictureSizes == null || supportPreviewSizes == null
                || supportPictureSizes.isEmpty() || supportPreviewSizes.isEmpty()) {
            return null;
        }

        // 先找到和屏幕宽高比一直的sizes
        int screenMax = Math.max(screenWidth, screenHeight);
        int screenMin = Math.min(screenWidth, screenHeight);
        float screenRatio = (float) screenMax / screenMin;

        // 和屏幕宽高比一致的PictureSize
        List<Camera.Size> matchedWantedSizeRatioPictureList = getMatchedRatioSize(supportPictureSizes, screenRatio);
        // 和屏幕宽高比一致的PreviewSize
        List<Camera.Size> matchedWantedSizeRatioPreviewList = getMatchedRatioSize(supportPreviewSizes, screenRatio);

        if(matchedWantedSizeRatioPictureList != null && !matchedWantedSizeRatioPictureList.isEmpty()
                && matchedWantedSizeRatioPreviewList != null && !matchedWantedSizeRatioPreviewList.isEmpty()) {
            // 找出和屏幕一致的Size后，继续去找和wanted相匹配的最佳的Size
            Camera.Size pictureSize = getBestMatchedSize(matchedWantedSizeRatioPictureList, wantedWidth, wantedHeight, 0.3f, 0.2f);
            Camera.Size previewSize = getBestMatchedSize(matchedWantedSizeRatioPreviewList, wantedWidth, wantedHeight, 1.5f, 0.2f);

            if(pictureSize != null && previewSize != null) {
                CustomSize[] matchedSize = new CustomSize[2];
                matchedSize[0] = new CustomSize(pictureSize.width, pictureSize.height);
                matchedSize[1] = new CustomSize(previewSize.width, previewSize.height);
                return matchedSize;
            }
        }
        return null;
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

    /**
     * 设置预览Size
     */
    public boolean setPreviewSize(CustomSize size) {
        if(mCamera == null) {
            Log.e(TAG, "Camera is null, setPreviewSize failed.");
            return false;
        }
        Camera.Parameters params = getCameraParameters();
        if(params == null) {
            Log.e(TAG, "Parameters is null, setPreviewSize failed.");
            return false;
        }

        try {
            params.setPreviewSize(size.width, size.height);
            boolean result = setCameraParameters(params);
            if(result) {
                mPreviewSize = size;
                return true;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 设置拍照Size
     */
    public boolean setPictureSize(CustomSize size) {
        if(mCamera == null) {
            Log.e(TAG, "Camera is null, setPictureSize failed.");
            return false;
        }
        Camera.Parameters params = getCameraParameters();
        if(params == null) {
            Log.e(TAG, "Parameters is null, setPictureSize failed.");
            return false;
        }

        try {
            params.setPictureSize(size.width, size.height);
            mPictureSize = size;
            boolean result = setCameraParameters(params);
            if(result) {
                mPictureSize = size;
                return true;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

        return false;
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
