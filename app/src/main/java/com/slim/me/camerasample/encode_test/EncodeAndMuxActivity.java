package com.slim.me.camerasample.encode_test;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.slim.me.camerasample.R;

import java.io.IOException;

/**
 * 自配EGL环境 + MediaCodec录制example
 * Created by slimxu on 2018/1/3.
 */

public class EncodeAndMuxActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encode_and_mux);

        EncodeAndMux mux = new EncodeAndMux();
        try {
            mux.startEncodeMp4();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
