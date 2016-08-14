package com.syd.oden.gesturelock.view.listener;

/**
 * 项目名称：MeshLed_dxy
 * 类描述：
 * 创建人：oden
 * 创建时间：2016/7/25 20:41
 */
public interface GesturePasswordSettingListener {
    boolean onFirstInputComplete(int len);
    void onSuccess();
    void onFail();
}
