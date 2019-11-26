package com.slim.me.camerasample.util;

public class FastClickUtils {

    private static final long DEFAULT_INTERVAL = 500;
    //允许当前控件快速点击
    public static final int ALLOW_FAST_CLICK = 0;
    //不允许当前控件快速点击
    public static final int FORBID_FAST_CLICK = 1;
    private static int sLatestEnter;
    /**
     * 上次click的时间
     */
    private static long sLastClickTime;

    /**
     * 是否是快速点击
     */
    public static synchronized boolean isFastClick(int isAllowed) {
        boolean isFastClick = false;
        long curClickTime = System.currentTimeMillis();
        if ((isAllowed != ALLOW_FAST_CLICK || sLatestEnter != isAllowed)
                && (curClickTime - sLastClickTime) <= DEFAULT_INTERVAL){
            isFastClick = true;
        }
        sLastClickTime = curClickTime;
        sLatestEnter = isAllowed;
        return isFastClick;
    }

}
