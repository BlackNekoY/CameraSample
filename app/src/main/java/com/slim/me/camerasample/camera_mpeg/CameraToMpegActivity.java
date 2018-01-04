package com.slim.me.camerasample.camera_mpeg;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.slim.me.camerasample.R;

/**
 * Created by slimxu on 2018/1/3.
 */

public class CameraToMpegActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_to_mpeg);

        CameraToMpegTest test = new CameraToMpegTest();
        try {
            test.testEncodeCameraToMp4();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
