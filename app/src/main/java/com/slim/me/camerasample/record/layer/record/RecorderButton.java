package com.slim.me.camerasample.record.layer.record;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.PathInterpolatorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.slim.me.camerasample.task.WeakHandler;
import com.slim.me.camerasample.util.FastClickUtils;
import com.slim.me.camerasample.util.ToastUtils;
import com.slim.me.camerasample.util.UIUtil;


public class RecorderButton extends View implements WeakHandler.IHandler {

    private final static String TAG = "RecorderButton";
    private static final int DEFAULT_WHITE_STROKE_WIDTH = 3;
    private static final float DEFAULT_RED_CIRCLE_SIZE_IDLE = 60;
    private static final float DEFAULT_RED_CIRCLE_SIZE_RECORDING = 62;


    public interface CameraPrepareResult {
        boolean isCameraReady();
    }

    public static final int STATUS_START = 1;
    public static final int STATUS_PAUSE = 2;
    public static final int STATUS_RECORDING = 3;
    private static final int SINGLE_CLICK_INTERVAL = 300;
    private static final int HANDLER_START_LONG_CLICK = 0;

    private Paint mRedPaint;
    private Paint mWhitePaint;
    private Paint mGrayPaint;
    private Paint mErrorPaint;
    private Paint mPausePaint;
    private Paint mTextPaint;
    private int mMeasureWidth = -1;
    private float mNormalSize;
    private int mRecorderStatus = STATUS_PAUSE;
    private OnRecorderButtonListener mListener;
    private boolean mCanTouchable = true;
    private boolean mEnable = true;
    private boolean mIsError = false;

    private long mLastClickRecordTime;
    private static final int MIN_CLICK_DURATION = 300;
    private WeakHandler mHandler;
    private boolean mCanPause = true;
    private CameraPrepareResult mCameraPrepareResult;
    private boolean mConutdownSupported = false;
    private boolean mClickable = true;
    private boolean mDrawPauseIcon = true;
    private Drawable mCoverDrawable = null;
    private int mCoverWidth;
    private int mCoverHeight;

    private String mMaxLengthHint = "视频已经录制到最大时长";

    public RecorderButton(Context context) {
        super(context);
        init();
    }

    public RecorderButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RecorderButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mNormalSize = UIUtil.dip2px(getContext(),66f);

        mHandler = new WeakHandler(this);
        mRedPaint = new Paint();
        mWhitePaint = new Paint();
        mGrayPaint = new Paint();
        mErrorPaint = new Paint();
        mPausePaint = new Paint();
        mTextPaint = new Paint();
        mTextPaint.setFakeBoldText(true);
        initPaint(mRedPaint, Color.parseColor("#f85959"), -1);
        initPaint(mWhitePaint, Color.parseColor("#ffffff"), DEFAULT_WHITE_STROKE_WIDTH);
        initPaint(mGrayPaint, Color.parseColor("#dddddd"), -1);
        initPaint(mErrorPaint, Color.parseColor("#cacaca"), -1);
        initPaint(mPausePaint, Color.parseColor("#ffffff"), -1);
        initPaint(mTextPaint, Color.parseColor("#ffffff"),-1);
    }

    private void initPaint(Paint paint, @ColorInt int color, int strokeWidth) {
        paint.setAntiAlias(true);
        paint.setColor(color);
        if (strokeWidth > 0) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(UIUtil.dip2px(getContext(),strokeWidth));
        } else {
            paint.setStyle(Paint.Style.FILL);
        }
    }

    public void setClickable(boolean clickable) {
        mClickable = clickable;
    }

    public void setCoverDrawable(Drawable drawable) {
        if (mCoverDrawable != drawable) {
            mCoverDrawable = drawable;
            mCoverWidth = mCoverDrawable.getIntrinsicWidth();
            mCoverHeight = mCoverDrawable.getIntrinsicHeight();
            mCoverDrawable.setBounds(0, 0, mCoverWidth, mCoverHeight);
        }
    }

    public void setMaxLengthHint(@NonNull String hint) {
        mMaxLengthHint = hint;
    }

    public void setDrawPauseIcon(boolean draw) {
        mDrawPauseIcon = draw;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mClickable) return true;
        if (!isInValidArea(event) && event.getAction() == MotionEvent.ACTION_DOWN) {
            return super.onTouchEvent(event);
        }
        if (!mCanPause && !mEnable && mRecorderStatus == STATUS_START) {
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            cancelLongClickEvent();
        }
        long time = System.currentTimeMillis();
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (time - mLastClickRecordTime <= MIN_CLICK_DURATION) {
                return true;
            }
        }
        if (mIsError ||
                (mCameraPrepareResult != null && !mCameraPrepareResult.isCameraReady())) {
            return true;
        }
        if (!mCanTouchable) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                ToastUtils.showToast(getContext(), mMaxLengthHint);
            }
            return true;
        }
        if (mConutdownSupported && mRecorderStatus == STATUS_PAUSE) {
            if(event.getAction() == MotionEvent.ACTION_DOWN && !FastClickUtils.isFastClick(FastClickUtils.ALLOW_FAST_CLICK)) {
                if(mListener != null) mListener.onCountDownStart();
            }
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mRecorderStatus == STATUS_PAUSE) {
                    mHandler.sendEmptyMessageDelayed(HANDLER_START_LONG_CLICK, SINGLE_CLICK_INTERVAL);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_CANCEL:
                cancelLongClickEvent();
                break;
            case MotionEvent.ACTION_UP:
                cancelLongClickEvent();
                switch (mRecorderStatus) {
                    case STATUS_RECORDING:
                    case STATUS_START:
                        onStopRecorder(mRecorderStatus == STATUS_RECORDING);
                        break;
                    case STATUS_PAUSE:
                        onStartRecorder();
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
        return true;
    }

    private void cancelLongClickEvent() {
        if (mHandler.hasMessages(HANDLER_START_LONG_CLICK)) {
            mHandler.removeMessages(HANDLER_START_LONG_CLICK);
        }
    }

    private boolean isInValidArea(MotionEvent event) {
        int forty = UIUtil.dip2px(getContext(),40);
        return event.getX() <= mMeasureWidth / 2 + forty
                && event.getX() >= mMeasureWidth / 2 - forty
                && event.getY() <= mMeasureWidth / 2 + forty
                && event.getY() >= mMeasureWidth / 2 - forty;
    }

    private void onHoldRecorder() {
        if (mListener != null) {
            boolean start = mListener.onHoldRecorder();
            if (start) {
                mRecorderStatus = STATUS_RECORDING;
                holdAnimation();
                invalidate();
            }
            updateLastActionTime();
        }
    }

    public void setConutdownSupport(boolean mIsConutdownOn) {
        mConutdownSupported = mIsConutdownOn;
    }

    public void onStartRecorder() {
        if (mListener != null) {
            Log.d(TAG, "onStartRecorder");
            boolean start = mListener.onStartRecorder();
            if (start) {
                mRecorderStatus = STATUS_START;
                startAnimation(1.0f, 0.5f);
                invalidate();
            }
            updateLastActionTime();
        }
    }

    public void onStopRecorder(boolean isLongClick) {
        if (mListener != null) {
            Log.d(TAG, "onStopRecorder");
            if (mCanPause) {
                mListener.onStopRecorder(isLongClick);
            } else {
                mListener.onFinish(isLongClick);
            }
            mRecorderStatus = STATUS_PAUSE;
            startAnimation(0.5f, 1.0f);
            invalidate();
            updateLastActionTime();
        }
    }

    private void updateLastActionTime() {
        mLastClickRecordTime = System.currentTimeMillis();
    }

    public void setListener(OnRecorderButtonListener listener) {
        mListener = listener;
    }

    public void setCanPause(boolean canPause) {
        mCanPause = canPause;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mMeasureWidth == -1) {
            mMeasureWidth = getMeasuredWidth();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mRecorderStatus == STATUS_PAUSE) {
            drawPause(canvas);
        } else if (mRecorderStatus == STATUS_START) {
            if (mCanPause) {
                drawStart(canvas);
            } else {
                drawStartWithoutPause(canvas);
            }
        } else if (mRecorderStatus == STATUS_RECORDING) {
            drawLongClick(canvas);
        }
    }

    public void setRecorderStatus(int status) {
        mRecorderStatus = status;
        if (mRecorderStatus == STATUS_PAUSE) {
            cancelHoldAnimator();
            if (mValueAnimator != null) {
                mValueAnimator.cancel();
            }
            mAnimatorFactor = 1;
        }
        invalidate();
    }

    private void drawPause(Canvas canvas) {
        mGrayAlphaFactor = 0.5f;
        int whiteStrokeWidth = UIUtil.dip2px(getContext(), DEFAULT_WHITE_STROKE_WIDTH);
        mWhitePaint.setStrokeWidth(whiteStrokeWidth);
        mWhitePaint.setAlpha(255);
        //draw outer circle
        canvas.drawCircle(mMeasureWidth / 2, mMeasureWidth / 2, mNormalSize / 2 * mAnimatorFactor - whiteStrokeWidth / 2, mWhitePaint);
        //draw inner circle
        canvas.drawCircle(mMeasureWidth / 2, mMeasureWidth / 2, UIUtil.dip2px(getContext(),DEFAULT_RED_CIRCLE_SIZE_IDLE / 2), mIsError ? mErrorPaint : mRedPaint);

        //draw cover if set
        if (mCoverDrawable != null) {
            final int saveCount = canvas.save();
            canvas.translate((mMeasureWidth - mCoverWidth) / 2, (mMeasureWidth - mCoverHeight) / 2);
            mCoverDrawable.draw(canvas);
            canvas.restoreToCount(saveCount);
        }
    }

    private void drawStart(Canvas canvas) {
        mGrayAlphaFactor = 0.5f;
        int whiteStrokeWidth = UIUtil.dip2px(getContext(),DEFAULT_WHITE_STROKE_WIDTH);
        mWhitePaint.setStrokeWidth(whiteStrokeWidth);
        mWhitePaint.setAlpha(255);
        canvas.drawCircle(mMeasureWidth / 2, mMeasureWidth / 2, mNormalSize / 2 * mAnimatorFactor - whiteStrokeWidth / 2, mWhitePaint);

        int lineWidth = UIUtil.dip2px(getContext(),3);
        int lineHeight = UIUtil.dip2px(getContext(),18);
        int lineMargin = UIUtil.dip2px(getContext(),8);
        float pauseIconFactor = 1.5f - mAnimatorFactor;
        mPausePaint.setAlpha((int) (255 * pauseIconFactor));
        canvas.drawCircle(mMeasureWidth / 2, mMeasureWidth / 2, UIUtil.dip2px(getContext(),DEFAULT_RED_CIRCLE_SIZE_RECORDING / 2), mRedPaint);
        if (mDrawPauseIcon) {
            if (UIUtil.isAboveLollipop()) {
                float left = mMeasureWidth / 2 - lineMargin / (4 * mAnimatorFactor) - lineWidth;
                float top = mMeasureWidth / 2 - lineHeight / (4 * mAnimatorFactor);
                float right = mMeasureWidth / 2 - lineMargin / 2;// - lineWidth * (mAnimatorFactor - 0.5f);
                float bottom = mMeasureWidth / 2 + lineHeight / (4 * mAnimatorFactor);
                //绘制圆角矩形
                canvas.drawRoundRect(left, top,
                        right, bottom, UIUtil.dip2px(getContext(),1f), UIUtil.dip2px(getContext(),1f), mPausePaint);
                left = mMeasureWidth / 2 + lineMargin / 2;// + lineWidth * (mAnimatorFactor - 0.5f);
                top = mMeasureWidth / 2 - lineHeight / (4 * mAnimatorFactor);
                right = mMeasureWidth / 2 + lineMargin / (4 * mAnimatorFactor) + lineWidth;
                bottom = mMeasureWidth / 2 + lineHeight / (4 * mAnimatorFactor);
                canvas.drawRoundRect(left, top,
                        right, bottom, UIUtil.dip2px(getContext(),1f), UIUtil.dip2px(getContext(),1f), mPausePaint);
            } else {
                //绘制圆角矩形
                canvas.drawLine(mMeasureWidth / 2 - lineMargin / 2 - lineWidth / 2, mMeasureWidth / 2 - lineHeight / (4 * mAnimatorFactor),
                        mMeasureWidth / 2 - lineMargin / 2 - lineWidth / 2, mMeasureWidth / 2 + lineHeight / (4 * mAnimatorFactor), mPausePaint);
                canvas.drawLine(mMeasureWidth / 2 + lineMargin / 2 + lineWidth / 2, mMeasureWidth / 2 - lineHeight / (4 * mAnimatorFactor),
                        mMeasureWidth / 2 + lineMargin / 2 + lineWidth / 2, mMeasureWidth / 2 + lineHeight / (4 * mAnimatorFactor), mPausePaint);
            }
        }
    }

    private void drawStartWithoutPause(Canvas canvas) {
        float pauseIconFactor = 1.5f - mAnimatorFactor;
        //画红底
        mRedPaint.setAlpha(mEnable ? 255 : (int) (255 * 0.5));
        canvas.drawCircle(mMeasureWidth / 2, mMeasureWidth / 2, UIUtil.dip2px(getContext(),DEFAULT_RED_CIRCLE_SIZE_RECORDING / 2), mRedPaint);

        mTextPaint.setAlpha(mEnable ? 255 : (int) (255 * 0.5));
        mTextPaint.setTextSize(UIUtil.dip2px(getContext(),20) * pauseIconFactor);
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        float fontHeight = fontMetrics.bottom - fontMetrics.top; //文字高度
        float textBaseY = mMeasureWidth - (mMeasureWidth - fontHeight) / 2 - fontMetrics.bottom;
        String string = "完成";
        float fontWidth = mTextPaint.measureText(string); //文字宽度
        float textBaseX = (mMeasureWidth - fontWidth) / 2;
        canvas.drawText(string, textBaseX, textBaseY, mTextPaint);
    }

    private void drawLongClick(Canvas canvas) {
        //画白圈
        int whiteStokeWidth = UIUtil.dip2px(getContext(),DEFAULT_WHITE_STROKE_WIDTH);
        mWhitePaint.setStrokeWidth(whiteStokeWidth);
        mWhitePaint.setAlpha(255);
        canvas.drawCircle(mMeasureWidth / 2, mMeasureWidth / 2, mNormalSize / 2 * mWhiteCircleFactor - whiteStokeWidth / 2, mWhitePaint);

        //画gray圈
        mGrayPaint.setAlpha((int) (mGrayAlphaFactor * 255));
        canvas.drawCircle(mMeasureWidth / 2, mMeasureWidth / 2, mMeasureWidth / 2 * mGrayCircleFactor, mGrayPaint);

        //画红圈
        canvas.drawCircle(mMeasureWidth / 2, mMeasureWidth / 2, UIUtil.dip2px(getContext(),DEFAULT_RED_CIRCLE_SIZE_RECORDING / 2) * mRedCircleFactor, mRedPaint);
//        canvas.drawCircle(mMeasureWidth / 2, mMeasureWidth / 2, UIUtil.dip2px(getContext(),32), mRedPaint);
    }

    private float mAnimatorFactor = 1;
    private ValueAnimator mValueAnimator;

    private void startAnimation(float start, float end) {
        cancelHoldAnimator();
        if (mValueAnimator != null) {
            mValueAnimator.cancel();
        }
        mValueAnimator = ValueAnimator.ofFloat(start, end).setDuration(400);
        mValueAnimator.setInterpolator(PathInterpolatorCompat.create(0.14f, 1, 0.34f, 1));
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mAnimatorFactor = (Float) animation.getAnimatedValue();
                Log.d(TAG, "AnimatorFactor = " + mAnimatorFactor);
                invalidate();
            }
        });
        mValueAnimator.start();
    }

    private float mWhiteCircleFactor;
    ValueAnimator mWhiteAnimator;
    private float mRedCircleFactor;
    ValueAnimator mRedAnimator;
    private float mGrayCircleFactor;
    ValueAnimator mGrayRepeatAnimator;
    ValueAnimator mGrayAnimator;
    private float mGrayAlphaFactor = 0.5f;
    ValueAnimator mGrayAlphaAnimator;

    private void holdAnimation() {
        if (mWhiteAnimator == null) {
            mWhiteAnimator = ValueAnimator.ofFloat(1.0f, 0f).setDuration(800);
            mWhiteAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mWhiteCircleFactor = (Float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            mWhiteAnimator.setInterpolator(PathInterpolatorCompat.create(0.14f, 1, 0.34f, 1));
        }
        mWhiteAnimator.start();

        if (mRedAnimator == null) {
            mRedAnimator = ValueAnimator.ofFloat(1.0f, 0.781f).setDuration(800);
            mRedAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mRedCircleFactor = (Float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            mRedAnimator.setInterpolator(PathInterpolatorCompat.create(0.14f, 1, 0.34f, 1));
        }
        mRedAnimator.start();

        if (mGrayAnimator == null) {
            mGrayAnimator = ValueAnimator.ofFloat(0f, 1f).setDuration(800);
            mGrayAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mGrayCircleFactor = (Float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            mGrayAnimator.setInterpolator(PathInterpolatorCompat.create(0.14f, 1, 0.34f, 1));
        }
        mGrayAnimator.start();

        if (mGrayRepeatAnimator == null) {
            mGrayRepeatAnimator = ValueAnimator.ofFloat(1f, 0.7f).setDuration(1000);
            mGrayRepeatAnimator.setStartDelay(800);
            mGrayRepeatAnimator.setRepeatMode(ValueAnimator.REVERSE);
            mGrayRepeatAnimator.setRepeatCount(ValueAnimator.INFINITE);
            mGrayRepeatAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mGrayCircleFactor = (Float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            mGrayRepeatAnimator.setInterpolator(PathInterpolatorCompat.create(0.48f, 0.04f, 0.52f, 0.96f));
        }
        mGrayRepeatAnimator.start();

        if (mGrayAlphaAnimator == null) {
            mGrayAlphaAnimator = ValueAnimator.ofFloat(0.5f, 1.0f).setDuration(1000);
            mGrayAlphaAnimator.setStartDelay(800);
            mGrayAlphaAnimator.setRepeatMode(ValueAnimator.REVERSE);
            mGrayAlphaAnimator.setRepeatCount(ValueAnimator.INFINITE);
            mGrayAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mGrayAlphaFactor = (Float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            mGrayAlphaAnimator.setInterpolator(PathInterpolatorCompat.create(0.48f, 0.04f, 0.52f, 0.96f));
        }
        mGrayAlphaAnimator.start();
    }

    private void cancelHoldAnimator() {
        if (mGrayAnimator != null) {
            mGrayAnimator.cancel();
        }
        if (mRedAnimator != null) {
            mRedAnimator.cancel();
        }
        if (mWhiteAnimator != null) {
            mWhiteAnimator.cancel();
        }
        if (mGrayRepeatAnimator != null) {
            mGrayRepeatAnimator.cancel();
        }
        if (mGrayAlphaAnimator != null) {
            mGrayAlphaAnimator.cancel();
        }
    }

    public void setState(boolean canTouchAble,boolean enable) {
        mCanTouchable = canTouchAble;
        mEnable = enable;
        if (!canTouchAble) {
            mRecorderStatus = STATUS_PAUSE;
            mAnimatorFactor = 1;
            cancelLongClickEvent();
        }
        postInvalidate();
    }

    public void setError(boolean isError) {
        if (mIsError == isError) {
            return;
        }
        mIsError = isError;
        postInvalidate();
    }

    public void setCameraPrepareResult(CameraPrepareResult result) {
        mCameraPrepareResult = result;
    }

    @Override
    public void handleMsg(Message msg) {
        switch (msg.what) {
            case HANDLER_START_LONG_CLICK:
                onHoldRecorder();
                break;
            default:
                break;
        }
    }

    public interface OnRecorderButtonListener {

        /**
         * 点击开始录制
         * @return true 表示执行此次start操作，false 阻止此次start操作
         */
        boolean onStartRecorder();

        /**
         * 停止录制
         * @param isLongClick
         */
        void onStopRecorder(boolean isLongClick);

        /**
         * 长按开始录制
         * @return true 表示执行此次start操作，false 阻止此次start操作
         */
        boolean onHoldRecorder();

        /**
         * 点击开始倒计时
         */
        void onCountDownStart();

        void onFinish(boolean isLongClick);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
