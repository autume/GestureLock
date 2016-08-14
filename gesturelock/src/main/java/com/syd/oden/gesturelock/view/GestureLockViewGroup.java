package com.syd.oden.gesturelock.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.syd.oden.gesturelock.R;
import com.syd.oden.gesturelock.view.GestureLockView.Mode;
import com.syd.oden.gesturelock.view.listener.GesturePasswordSettingListener;
import com.syd.oden.gesturelock.view.listener.GestureUnmatchedExceedListener;
import com.syd.oden.gesturelock.view.listener.GestureEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * 整体包含n*n个GestureLockView,每个GestureLockView间间隔mMarginBetweenLockView，
 * 最外层的GestureLockView与容器存在mMarginBetweenLockView的外边距
 * <p/>
 * 关于GestureLockView的边长（n*n）： n * mGestureLockViewWidth + ( n + 1 ) *
 * mMarginBetweenLockView = mWidth ; 得：mGestureLockViewWidth = 4 * mWidth / ( 5
 * * mCount + 1 ) 注：mMarginBetweenLockView = mGestureLockViewWidth * 0.25 ;
 */
public class GestureLockViewGroup extends RelativeLayout {

    private static final String TAG = "GestureLockViewGroup";
    /**
     * 保存所有的GestureLockView
     */
    private GestureLockView[] mGestureLockViews;
    /**
     * 每个边上的GestureLockView的个数
     */
    private int mCount = 3;
    /**
     * 存储答案
     */
    private String password = "";
    /**
     * 保存用户选中的GestureLockView的id
     */
    private List<Integer> mChoose = new ArrayList<Integer>();
    private String mChooseString = "";

    private Paint mPaint;
    /**
     * 每个GestureLockView中间的间距 设置为：mGestureLockViewWidth * 25%
     */
    private int mMarginBetweenLockView = 30;
    /**
     * GestureLockView的边长 4 * mWidth / ( 5 * mCount + 1 )
     */
    private int mGestureLockViewWidth;

    /**
     * GestureLockView无手指触摸的状态下圆的颜色
     */
    private int mNoFingerColor = 0xFF378FC9;

    /**
     * GestureLockView手指触摸的状态下圆的颜色
     */
    private int mFingerOnColor = 0XFFEC159F;
    /**
     * GestureLockView手指抬起的状态下,正确时圆的颜色
     */
    private int mFingerUpColorCorrect = 0xFF91DC5A;

    /**
     * GestureLockView手指抬起的状态下，错误圆的颜色
     */
    private int mFingerUpColorError = 0xFFFF0000;
    /**
     * 宽度
     */
    private int mWidth;
    /**
     * 高度
     */
    private int mHeight;

    private Path mPath;
    /**
     * 指引线的开始位置x
     */
    private int mLastPathX;
    /**
     * 指引线的开始位置y
     */
    private int mLastPathY;
    /**
     * 指引下的结束位置
     */
    private Point mTmpTarget = new Point();

    /**
     * 最大尝试次数
     */
    private int mTryTimes = -1;
    public static boolean isCorrect = false;

    /**
     * 回调接口
     */
    private GesturePasswordSettingListener gesturePasswordSettingListener;
    private GestureEventListener gestureEventListener;
    private GestureUnmatchedExceedListener gestureUnmatchedExceedListener;

    private GesturePreference gesturePreference;
    private boolean isSetPassword = false;
    private boolean isInPasswordSettingMode = false;
    private boolean isWaitForFirstInput = true;
    private boolean isRetryTimeLimit = false;
    private String firstInputPassword = "";
    private int mPrferenceId = -1;

    public GestureLockViewGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureLockViewGroup(Context context, AttributeSet attrs,
                                int defStyle) {
        super(context, attrs, defStyle);

        /**
         * 获得所有自定义的参数的值
         */
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.GestureLockViewGroup, defStyle, 0);

        mNoFingerColor = a.getColor(R.styleable.GestureLockViewGroup_color_no_finger, mNoFingerColor);
        mFingerOnColor = a.getColor(R.styleable.GestureLockViewGroup_color_finger_on, mFingerOnColor);
        mFingerUpColorCorrect = a.getColor(R.styleable.GestureLockViewGroup_color_finger_up_correct, mFingerUpColorCorrect);
        mFingerUpColorError = a.getColor(R.styleable.GestureLockViewGroup_color_finger_up_error, mFingerUpColorError);
        mCount = a.getInt(R.styleable.GestureLockViewGroup_count, mCount);
        mPrferenceId = a.getInt(R.styleable.GestureLockViewGroup_preference_id, mPrferenceId);

        a.recycle();

        /**
         * 获取密码状态
         */
        gesturePreference = new GesturePreference(context, mPrferenceId);
        password = gesturePreference.ReadStringPreference();
        Log.d(TAG, "password now is : " + password);
        isSetPassword = !password.equals("null"); //判断是否已经保存有密码
        isInPasswordSettingMode = !isSetPassword;     //当未设置密码，进入密码设置模式

        // 初始化画笔
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPath = new Path();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);

        mHeight = mWidth = mWidth < mHeight ? mWidth : mHeight;
        initViews();
    }

    private void initViews() {
        // 初始化mGestureLockViews
        if (mGestureLockViews == null) {
            mGestureLockViews = new GestureLockView[mCount * mCount];
            // 计算每个GestureLockView的宽度
            mGestureLockViewWidth = (int) (4 * mWidth * 1.0f / (5 * mCount + 1));
            //计算每个GestureLockView的间距
            mMarginBetweenLockView = (int) (mGestureLockViewWidth * 0.25);
            // 设置画笔的宽度为GestureLockView的内圆直径稍微小点
            mPaint.setStrokeWidth(mGestureLockViewWidth * 0.29f);

            for (int i = 0; i < mGestureLockViews.length; i++) {
                //初始化每个GestureLockView
                mGestureLockViews[i] = new GestureLockView(getContext(), mNoFingerColor, mFingerOnColor, mFingerUpColorCorrect, mFingerUpColorError);
                mGestureLockViews[i].setId(i + 1);
                //设置参数，主要是定位GestureLockView间的位置
                RelativeLayout.LayoutParams lockerParams = new RelativeLayout.LayoutParams(
                        mGestureLockViewWidth, mGestureLockViewWidth);

                // 不是每行的第一个，则设置位置为前一个的右边
                if (i % mCount != 0) {
                    lockerParams.addRule(RelativeLayout.RIGHT_OF,
                            mGestureLockViews[i - 1].getId());
                }
                // 从第二行开始，设置为上一行同一位置View的下面
                if (i > mCount - 1) {
                    lockerParams.addRule(RelativeLayout.BELOW,
                            mGestureLockViews[i - mCount].getId());
                }
                //设置右下左上的边距
                int rightMargin = mMarginBetweenLockView;
                int bottomMargin = mMarginBetweenLockView;
                int leftMagin = 0;
                int topMargin = 0;
                /**
                 * 每个View都有右外边距和底外边距 第一行的有上外边距 第一列的有左外边距
                 */
                if (i >= 0 && i < mCount)// 第一行
                {
                    topMargin = mMarginBetweenLockView;
                }
                if (i % mCount == 0)// 第一列
                {
                    leftMagin = mMarginBetweenLockView;
                }

                lockerParams.setMargins(leftMagin, topMargin, rightMargin,
                        bottomMargin);
                mGestureLockViews[i].setMode(Mode.STATUS_NO_FINGER);
                addView(mGestureLockViews[i], lockerParams);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();
        Log.d(TAG, "mTryTimes : " + mTryTimes);

        //重试次数超过限制，直接返回
        if (mTryTimes <= 0 && isRetryTimeLimit) {
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                reset();     // 重置
                break;
            case MotionEvent.ACTION_MOVE:
                drawAndGetSelectedWhenTouchMove(x, y);
                break;
            case MotionEvent.ACTION_UP:
                if (isInPasswordSettingMode) {
                    if (gesturePasswordSettingListener != null)
                        setPasswordHandle();  //设置密码
                } else {
                    if (mChoose.size() > 0) {
                        isCorrect = checkAnswer();
                    } else {
                        return true;
                    }

                    if (gestureEventListener != null) {
                        gestureEventListener.onGestureEvent(isCorrect);  //将结果回调
                    }
                    if (this.mTryTimes == 0) {
                        gestureUnmatchedExceedListener.onUnmatchedExceedBoundary();  //超出重试次数，进入回调
                    }
                }
                drawWhenTouchUp();
                break;
        }
        invalidate();
        return true;
    }

    private void drawAndGetSelectedWhenTouchMove(int x, int y) {
        mPaint.setColor(mFingerOnColor);
        mPaint.setAlpha(50);
        GestureLockView child = getChildIdByPos(x, y);
        if (child != null) {
            int cId = child.getId();
            if (!mChoose.contains(cId)) {
                mChoose.add(cId);
                mChooseString = mChooseString + cId;
                child.setMode(Mode.STATUS_FINGER_ON);
                // 设置指引线的起点
                mLastPathX = child.getLeft() / 2 + child.getRight() / 2;
                mLastPathY = child.getTop() / 2 + child.getBottom() / 2;

                if (mChoose.size() == 1)// 当前添加为第一个
                {
                    mPath.moveTo(mLastPathX, mLastPathY);
                } else
                // 非第一个，将两者使用线连上
                {
                    mPath.lineTo(mLastPathX, mLastPathY);
                }
            }
        }
        // 指引线的终点
        mTmpTarget.x = x;
        mTmpTarget.y = y;
    }

    private void drawWhenTouchUp() {
        if (isCorrect) {
            mPaint.setColor(mFingerUpColorCorrect);
        } else {
            mPaint.setColor(mFingerUpColorError);
        }
        mPaint.setAlpha(50);
        Log.d(TAG, "mChoose = " + mChoose);
        // 将终点设置位置为起点，即取消指引线
        mTmpTarget.x = mLastPathX;
        mTmpTarget.y = mLastPathY;

        // 改变子元素的状态为UP
        setItemModeUp();

        // 计算每个元素中箭头需要旋转的角度
        for (int i = 0; i + 1 < mChoose.size(); i++) {
            int childId = mChoose.get(i);
            int nextChildId = mChoose.get(i + 1);

            GestureLockView startChild = (GestureLockView) findViewById(childId);
            GestureLockView nextChild = (GestureLockView) findViewById(nextChildId);

            int dx = nextChild.getLeft() - startChild.getLeft();
            int dy = nextChild.getTop() - startChild.getTop();
            // 计算角度
            int angle = (int) Math.toDegrees(Math.atan2(dy, dx)) + 90;
            startChild.setArrowDegree(angle);
        }
    }

    private void setPasswordHandle() {
        if (isWaitForFirstInput) {
            if (gesturePasswordSettingListener.onFirstInputComplete(mChooseString.length())) {
                firstInputPassword = mChooseString;
                isWaitForFirstInput = false;
            }
        } else {
            if (firstInputPassword.equals(mChooseString)) {
                gesturePasswordSettingListener.onSuccess();
                savePassword(mChooseString);
                isInPasswordSettingMode = false;
            } else {
                gesturePasswordSettingListener.onFail();
            }
        }
        reset();
    }

    private void setItemModeUp() {
        for (GestureLockView gestureLockView : mGestureLockViews) {
            if (mChoose.contains(gestureLockView.getId())) {
                gestureLockView.setMode(Mode.STATUS_FINGER_UP);
            }
        }
    }

    /**
     * 检查用户绘制的手势是否正确
     *
     * @return
     */
    public boolean checkAnswer() {
        if (password.equals(mChooseString)) {
            return true;
        } else {
            if (isRetryTimeLimit)
                this.mTryTimes--;
            return false;
        }
    }

    /**
     * 通过x,y获得落入的GestureLockView
     *
     * @param x
     * @param y
     * @return
     */
    private GestureLockView getChildIdByPos(int x, int y) {
        for (GestureLockView gestureLockView : mGestureLockViews) {
            if (checkPositionInChild(gestureLockView, x, y)) {
                return gestureLockView;
            }
        }
        return null;
    }

    /**
     * 检查当前是否在child中
     *
     * @param child
     * @param x
     * @param y
     * @return
     */
    private boolean checkPositionInChild(View child, int x, int y) {

        //设置了内边距，即x,y必须落入下GestureLockView的内部中间的小区域中，可以通过调整padding使得x,y落入范围变大，或者不设置padding
        int padding = (int) (mGestureLockViewWidth * 0.15);

        if (x >= child.getLeft() + padding && x <= child.getRight() - padding
                && y >= child.getTop() + padding
                && y <= child.getBottom() - padding) {
            return true;
        }
        return false;
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        //绘制GestureLockView间的连线
        if (mPath != null) {
            canvas.drawPath(mPath, mPaint);
        }
        //绘制指引线
        if (mChoose.size() > 0) {
            if (mLastPathX != 0 && mLastPathY != 0)
                canvas.drawLine(mLastPathX, mLastPathY, mTmpTarget.x,
                        mTmpTarget.y, mPaint);
        }
    }

    /**
     * 做一些必要的重置
     */
    private void reset() {
        mChoose.clear();
        mChooseString = "";
        mPath.reset();
        for (GestureLockView gestureLockView : mGestureLockViews) {
            gestureLockView.setMode(Mode.STATUS_NO_FINGER);
            gestureLockView.setArrowDegree(-1);
        }
    }


    //对外公开的一些方法

    public void setGestureEventListener(GestureEventListener gestureEventListener) {
        this.gestureEventListener = gestureEventListener;
    }

    public void setGestureUnmatchedExceedListener(int retryTimes, GestureUnmatchedExceedListener gestureUnmatchedExceedListener) {
        isRetryTimeLimit = true;
        this.mTryTimes = retryTimes;
        this.gestureUnmatchedExceedListener = gestureUnmatchedExceedListener;
    }

    public void setGesturePasswordSettingListener(GesturePasswordSettingListener gesturePasswordSettingListener) {
        this.gesturePasswordSettingListener = gesturePasswordSettingListener;
    }

    public void removePassword() {
        gesturePreference.WriteStringPreference("null");
        this.isSetPassword = false;
        isWaitForFirstInput = true;
        isInPasswordSettingMode = true;
    }

    public void savePassword(String password) {
        this.password = password;
        gesturePreference.WriteStringPreference(password);
    }

    public String getPassword() {
        return password;
    }

    public void resetView() {
        reset();
        invalidate();
    }

    public void setRetryTimes(int retryTimes) {
        this.mTryTimes = retryTimes;
    }

    public boolean isSetPassword() {
        return isSetPassword;
    }

}
