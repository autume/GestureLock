# GestureLock
Android GestureLock
## 功能特点
- 支持手势密码的绘制，并支持密码保存功能，解锁时自动比对密码给出结果
- 封装了绘制密码的方法，比对两次密码是否一致，可以快捷地进行手势密码的设置
- 可以设置密码输入错误后的重试次数上限
- 可以自定义不同状态下手势密码图案的颜色
- 可以自定义手势密码的触摸点数量（n*n）

## 使用效果
demo：
![](http://i.imgur.com/P13oXgW.gif)

## 在项目中导入该库
在工程的 build.gradle中加入：
```java
allprojects {
		repositories {
			...
			maven { url "https://jitpack.io" }
		}
	}
```
module的build.gradle中加入依赖：
```java
dependencies {
	        compile 'com.github.autume:GestureLock:1.0.0'
	}
``
```

## 使用方法
### XML布局文件中使用该控件
```java
 <com.syd.oden.gesturelock.view.GestureLockViewGroup
        android:id="@+id/gesturelock"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:preference_id="1"
        android:layout_marginTop="30dp"
        app:count="3" />
```
可以设置的一些参数，说明如下:
color_no_finger:未触摸时圆形的颜色
color_finger_on:触摸时圆形的颜色
color_finger_up_correct:输入正确时圆形的颜色
color_finger_up_error:出错时圆形的颜色
count：收拾密码的圆形数量，n*n
preference_id：手势密码保存的id号，不输入或输入-1则使用默认的id

### 初始化
```java
private void initGesture() {
        mGestureLockViewGroup = (GestureLockViewGroup) findViewById(R.id.gesturelock);
        gestureEventListener();
        gesturePasswordSettingListener();
        gestureRetryLimitListener();
    }
```
### 设置手势密码监听事件
```java
 private void gestureEventListener() {
        mGestureLockViewGroup.setGestureEventListener(new GestureEventListener() {
            @Override
            public void onGestureEvent(boolean matched) {
                mylog.d("onGestureEvent matched: " + matched);
                if (!matched) {
                    tv_state.setTextColor(Color.RED);
                    tv_state.setText("手势密码错误");
                } else {
                    if (isReset) {
                        isReset = false;
                        Toast.makeText(MainActivity.this, "清除成功!", Toast.LENGTH_SHORT).show();
                        resetGesturePattern();
                    } else {
                        tv_state.setTextColor(Color.WHITE);
                        tv_state.setText("手势密码正确");
                    }
                }
            }
        });
    }
```
若已经设置有密码则会进入该回调，在这里对结果进行处理，上面的例子中加入了一个重设密码的处理。
### 手势密码设置
```java
 private void gesturePasswordSettingListener() {
        mGestureLockViewGroup.setGesturePasswordSettingListener(new GesturePasswordSettingListener() {
            @Override
            public boolean onFirstInputComplete(int len) {
                if (len > 3) {
                    tv_state.setTextColor(Color.WHITE);
                    tv_state.setText("再次绘制手势密码");
                    return true;
                } else {
                    tv_state.setTextColor(Color.RED);
                    tv_state.setText("最少连接4个点，请重新输入!");
                    return false;
                }
            }

            @Override
            public void onSuccess() {
                tv_state.setTextColor(Color.WHITE);
                Toast.makeText(MainActivity.this, "密码设置成功!", Toast.LENGTH_SHORT).show();
                tv_state.setText("请输入手势密码解锁!");
            }

            @Override
            public void onFail() {
                tv_state.setTextColor(Color.RED);
                tv_state.setText("与上一次绘制不一致，请重新绘制");
            }
        });
    }
```
若还未设置密码，绘制手势的时候会进入该回调，返回值为绘制的触摸点的数量，onFirstInputComplete中返回true则进入第二手势密码的绘制，两次输入一致后自动保存密码。
### 重试次数超过限制监听
```java
 private void gestureRetryLimitListener() {
        mGestureLockViewGroup.setGestureUnmatchedExceedListener(3, new GestureUnmatchedExceedListener() {
            @Override
            public void onUnmatchedExceedBoundary() {
                tv_state.setTextColor(Color.RED);
                tv_state.setText("错误次数过多，请稍后再试!");
            }
        });
    }
```
若设置了该监听事件，则输入错误有次数限制，超过上限后进入回调，在该回调中进行处理。

清除密码的逻辑自己加个判断处理下即可，具体可以看下demo

### 其他的一些API
public void removePassword() ：清除密码
public void savePassword() : 保存密码，设置手势密码成功后会自动保存，也可以调用该接口另外设置密码
public void getPassword()： 获取密码
public void setRetryTimes(int retryTimes) ： 设置重试次数上限
public boolean isSetPassword() ： 返回现在是否已经设置有密码
public void resetView() ： 将视图Reset
