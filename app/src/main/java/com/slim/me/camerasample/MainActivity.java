package com.slim.me.camerasample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import com.slim.me.camerasample.camera.CameraHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private Button mTakePicture;
    private FrameLayout mPreviewParent;
//    private CameraSurfaceView mCameraSurface;
    private CameraTextureView mCameraTexture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        mTakePicture = (Button) findViewById(R.id.take_picture);
        mTakePicture.setOnClickListener(this);
        mPreviewParent = (FrameLayout) findViewById(R.id.preview_parent);

        setupCameraPreviewView();
    }

    private void setupCameraPreviewView() {
        mPreviewParent.removeAllViews();

        //SurfaceView
//        mCameraSurface = new CameraSurfaceView(this);
//        mPreviewParent.addView(mCameraSurface, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

        //TextureView
        mCameraTexture = new CameraTextureView(this);
        mPreviewParent.addView(mCameraTexture, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        CameraHelper.getInstance().openCamera(CameraHelper.CAMERA_FRONT);
        mCameraTexture.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mCameraTexture.setVisibility(View.INVISIBLE);
        CameraHelper.getInstance().releaseCamera();
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
        }
    }
}
