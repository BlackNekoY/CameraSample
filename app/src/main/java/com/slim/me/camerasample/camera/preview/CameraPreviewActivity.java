package com.slim.me.camerasample.camera.preview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import com.slim.me.camerasample.R;
import com.slim.me.camerasample.camera.CameraHelper;
import com.slim.me.camerasample.util.BitmapUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by slimxu on 2018/1/3.
 */

public class CameraPreviewActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "CameraPreviewActivity";
    private static final boolean USE_SURFACE_PREVIEW = false;

    private View mCameraPreviewView;
    private Button mTakePicture;
    private Button mChangeCamera;
    private FrameLayout mPreviewParent;

    private PreviewContext mPreviewContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_camera_preview);

        mTakePicture = (Button) findViewById(R.id.take_picture);
        mTakePicture.setOnClickListener(this);
        mPreviewParent = (FrameLayout) findViewById(R.id.preview_parent);
        mChangeCamera = (Button) findViewById(R.id.change_camera);
        mChangeCamera.setOnClickListener(this);

        setupCameraPreviewView();
    }

    private void setupCameraPreviewView() {
        mPreviewParent.removeAllViews();

        if(Build.VERSION.SDK_INT >= 19 && !USE_SURFACE_PREVIEW) {
            TexturePreviewContext previewContext = new TexturePreviewContext(this);
            CameraTextureView textureView = new CameraTextureView(this);

            textureView.setPreviewContext(previewContext);

            mPreviewContext = previewContext;
            mCameraPreviewView = textureView;
        }else {
            SurfacePreviewContext previewContext = new SurfacePreviewContext(this);
            CameraSurfaceView surfaceView = new CameraSurfaceView(this);

            surfaceView.setPreviewContext(previewContext);

            mPreviewContext = previewContext;
            mCameraPreviewView = surfaceView;
        }

        mPreviewParent.addView(mCameraPreviewView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
//        mPreviewParent.addView(mCameraPreviewView, 0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.take_picture:
                Camera camera = CameraHelper.getInstance().getCamera();
                if(camera != null) {
                    camera.takePicture(null, null, new Camera.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                            Bitmap rotateBitmap = BitmapUtil.rotate(bitmap, 90);

                            File file = new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis() + ".jpg");
                            try {
                                FileOutputStream fos = new FileOutputStream(file);
                                rotateBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                                fos.close();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            camera.startPreview();
                        }
                    });
                }
                break;
            case R.id.change_camera:
                break;
        }
    }
}
