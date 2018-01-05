package com.slim.me.camerasample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

/**
 * Created by slimxu on 2017/11/16.
 */

public class MediaCodecActivity extends AppCompatActivity{

    private static final String MIME_TYPE = "video/avc";

    private Button mStartRecord;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_mediacodec);

        prepareEncoder();
    }

    private void prepareEncoder() {

    }
}
