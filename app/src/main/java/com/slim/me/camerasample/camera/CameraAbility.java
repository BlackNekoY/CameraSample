package com.slim.me.camerasample.camera;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Build;
import android.util.Log;

import java.util.List;

/**
 * 检查系统Camera提供的基本能力，通过{@link #bindCamera(Camera)}获取到Parameters <br/>
 * 由于Camera.getParameters 每次会新建一个实例返回，所以这个类的Parameters很可能不是最新的实例 <br/>
 * 这个类只提供一些基本的能力检查，比如是否提供auto focus，support size等，这些属性都不会随着对象的改变而改变 <br/>
 */
public class CameraAbility {

    public static final String TAG = "CameraAbility";

    private static final int INVLID_CAMERA_ID = -1;

    private static int mCameraNumbers = 0;
    private static int mBackCameraId = INVLID_CAMERA_ID;
    private static int mFrontCameraId = INVLID_CAMERA_ID;

    private Camera.Parameters mParameters;
    private static CameraAbility mInstance;

    static {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                mCameraNumbers = Camera.getNumberOfCameras();
                for (int i = 0; i < mCameraNumbers; ++i) {
                    CameraInfo info = new CameraInfo();
                    Camera.getCameraInfo(i, info);
                    if (null != info) {
                        if (CameraInfo.CAMERA_FACING_BACK == info.facing) {
                            mBackCameraId = i;
                        } else if (CameraInfo.CAMERA_FACING_FRONT == info.facing) {
                            mFrontCameraId = i;
                        }
                    }
                }
            } else {
                mCameraNumbers = 1;
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            mCameraNumbers = 1;
        }
    }

    public static CameraAbility getInstance() {
        if (mInstance == null) {
            synchronized (CameraAbility.class) {
                if (mInstance == null) {
                    mInstance = new CameraAbility();
                }
            }
        }
        return mInstance;
    }

    public static boolean hasCameras() {
        return mCameraNumbers > 0;
    }

    public static boolean hasBackCamera() {
        return hasCameras() && mBackCameraId != INVLID_CAMERA_ID;
    }

    public static boolean hasFrontCamera() {
        return hasCameras() && mFrontCameraId != INVLID_CAMERA_ID;
    }

    public static int getFrontCameraId() {
        return mFrontCameraId;
    }

    public static int getBackCameraId() {
        return mBackCameraId;
    }

    public boolean bindCamera(Camera camera) {
        reset();

        if (null == camera) {
            return false;
        }

        try {
            mParameters = camera.getParameters();
        } catch (Exception e) {
            e.printStackTrace();
            mParameters = null;
        }
        if (null == mParameters) {
            return false;
        }

        return true;
    }

    public boolean isSupportZoom() {
        assert (mParameters != null);

        try {
            return mParameters.isZoomSupported();
        } catch (Exception e) {
        }

        return false;
    }

    public boolean isSupportFocus(String mode) {
        assert (mParameters != null);

        try {
            List<String> focusModes = mParameters.getSupportedFocusModes();
            if (null == focusModes)//不支持会返回null
                return false;

            if (focusModes.contains(mode))
                return true;
        } catch (Exception e) {
        }

        return false;
    }

    public boolean hasFlashLight() {
        assert (mParameters != null);

        try {
            List<String> flashModes = mParameters.getSupportedFlashModes();
            if (null == flashModes)
                return false;

            if (flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH))
                return true;
        } catch (Exception e) {
        }

        return false;
    }

    public boolean isSupportPreviewFormat(int format) {
        assert (mParameters != null);

        try {
            List<Integer> formats = mParameters.getSupportedPreviewFormats();
            if (null == formats)
                return false;

            if (formats.contains(format))
                return true;
        } catch (Exception e) {
        }

        return false;
    }

    public List<Size> getPreviewSizes() {

        assert (mParameters != null);

        List<Size> sizes = null;
        try {
            sizes = mParameters.getSupportedPreviewSizes();
        } catch (Exception e) {
            e.printStackTrace();
            sizes = null;
        }

        if (sizes != null) {
            for (Size s : sizes) {
                if (s != null) {
                    Log.d(TAG, "[@] getPreviewSizes:w=" + s.width + ",h=" + s.height
                            + " w/h=" + (float) s.width / s.height);
                }
            }
        }
        return sizes;
    }

    public List<Size> getPictureSizes() {
        assert (mParameters != null);

        List<Size> sizes = null;
        try {
            sizes = mParameters.getSupportedPictureSizes();
        } catch (Exception e) {
            e.printStackTrace();
            sizes = null;
        }

        if (sizes != null) {
            for (Size s : sizes) {
                if (s != null) {
                    Log.d(TAG, "[@] getPictureSizes:w=" + s.width + ",h=" + s.height
                            + " w/h=" + (float) s.width / s.height);
                }
            }
        }
        return sizes;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public List<int[]> getPreviewFPS() {
        assert (mParameters != null);

        try {
            List<int[]> fpsRanges = mParameters.getSupportedPreviewFpsRange();
            return fpsRanges;
        } catch (Exception e) {
        }

        return null;
    }

    public void reset() {
        mParameters = null;
    }
}
