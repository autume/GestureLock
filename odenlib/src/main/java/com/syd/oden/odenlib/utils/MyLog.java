package com.syd.oden.odenlib.utils;

import android.content.Context;
import android.util.Log;

/**
 * 项目名称：OdenDemo
 * 类描述：
 * 创建人：oden
 * 创建时间：2016/6/12 15:49
 */
public class MyLog
{
    private  boolean isDebug = true;
    private  final String TAG = "OS";
    private  String myClassTag = "";
    private  StringBuffer classTag = new StringBuffer();

    public MyLog(String classTag) {
        this.classTag.append(classTag);
    }

    public MyLog(Context context) {
        this.classTag.append("[").append(context.getClass().getSimpleName()).append("] ");
    }

    public void d(String msg)
    {
        if (isDebug)
            Log.d(TAG, classTag + myClassTag + msg);
    }

    public void e(String msg)
    {
        if (isDebug)
            Log.e(TAG, classTag + myClassTag + msg);
    }

    public void w(String msg)
    {
        if (isDebug)
            Log.w(TAG, classTag + myClassTag + msg);
    }

    public void setMyClassTag(String myClassTag) {
        this.myClassTag = myClassTag;
    }

    public String getTag() {
        return classTag + myClassTag;
    }

}
