package com.slim.me.camerasample.app;

import android.os.Environment;

import java.io.File;

public class Constants {
    public static final String APP_DIR_NAME= "FilterDemo";
    public static final String APP_DIR_PATH = new File(Environment.getExternalStorageDirectory(), APP_DIR_NAME).getAbsolutePath();
}
