package com.example.wangchao.androidcamerabase.presenter;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.example.wangchao.androidcamerabase.base.BaseApplication;
import com.example.wangchao.androidcamerabase.camera.BaseCamera2Operator;
import com.example.wangchao.androidcamerabase.camera.Camera2Manager;
import com.example.wangchao.androidcamerabase.camera.utils.Camera2Utils;
import com.example.wangchao.androidcamerabase.contract.CameraContract;
import com.example.wangchao.androidcamerabase.imp.ICameraImp;
import com.example.wangchao.androidcamerabase.mode.Constant;
import com.example.wangchao.androidcamerabase.utils.permission.PermissionsManager;
import com.example.wangchao.androidcamerabase.utils.thread.WorkThreadUtils;
import com.example.wangchao.androidcamerabase.utils.time.TimingUtils;
import com.example.wangchao.androidcamerabase.utils.toast.ToastUtils;

import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class CameraPresenter implements CameraContract.Presenter, BaseCamera2Operator.Camera2ResultCallBack, BaseCamera2Operator.Camera2VideoRecordCallBack {
    private final String TAG = CameraPresenter.class.getSimpleName();
    private WorkThreadUtils workThreadManager;
    private Camera2Manager camera2Manager;
    private CameraContract.View mView;
    private CompositeSubscription compositeSubscription;
    private Context appContext;
    private int currentMode;
    private ICameraImp mICameraImp;
    private long time = 0;
    private Subscription cycleTimeSubscription;

    public CameraPresenter(CameraContract.View view, ICameraImp iCameraImp) {
        mICameraImp = iCameraImp;
        mView = view;
        compositeSubscription = new CompositeSubscription();
        workThreadManager = mICameraImp.getWordThreadManger();
        appContext = BaseApplication.getInstance();
        camera2Manager = mICameraImp.getCamera2Manager();
        camera2Manager.setCamera2ResultCallBack(this);
        camera2Manager.setCameraVideoCallBack(this);
        //默认拍照模式
        currentMode = Constant.MODE_CAMERA;
    }

    @Override
    public void switchMode(int mode) {
        if (mode == currentMode) {
            return;
        }
        currentMode = mode;
        switch (currentMode) {
            //切换到拍照模式
            case Constant.MODE_CAMERA:
                mView.showToast("正在切换到拍照模式");
                break;
            //切换到录像模式
            case Constant.MODE_VIDEO_RECORD:
                mView.showToast("正在切换到录像模式");
                break;
            default:
                break;
        }
        camera2Manager.switchMode(currentMode);
    }

    @Override
    public void switchCamera(int direction) {
        camera2Manager.switchCameraDirection(direction);
    }

    @Override
    public void onResume() {
        workThreadManager.startWorkThread();
        camera2Manager.onResume(mView.getCameraView());
    }

    @Override
    public void onPause() {
        camera2Manager.onPause();
        workThreadManager.stopBackgroundThread();
    }

    @Override
    public void takePictureOrVideo() {
        camera2Manager.takePictureOrVideo();
    }

    @Override
    public void callBack(Observable<String> result) {
        if (result != null) {
            Subscription subscription = result.subscribeOn(Schedulers.io())
                    .unsubscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<String>() {
                        @Override
                        public void call(String filePath) {
                            //通知图库，用于刷新
                            Camera2Utils.sendBroadcastNotify(mView.getCameraView().getContext(), filePath);
                            mView.loadPictureResult(filePath);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            //写入图片到磁盘失败
                            ToastUtils.showToast(BaseApplication.getInstance(), "写入磁盘失败");
                        }
                    });
            compositeSubscription.add(subscription);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PermissionsManager.CAMERA_REQUEST_CODE:
                //权限请求失败
                if (grantResults.length == PermissionsManager.CAMERA_REQUEST.length) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            ToastUtils.showToast(BaseApplication.getInstance(), "拍照权限被拒绝");
                            break;
                        }
                    }
                }
                break;
            case PermissionsManager.VIDEO_REQUEST_CODE:
                if (grantResults.length == PermissionsManager.VIDEO_PERMISSIONS.length) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            ToastUtils.showToast(BaseApplication.getInstance(), "录像权限被拒绝");
                            break;
                        }
                    }
                }
                break;
            default:
                break;
        }
    }


    @Override
    public void stopRecord() {
        if (cycleTimeSubscription != null) {
            compositeSubscription.remove(cycleTimeSubscription);
        }
        mView.switchRecordMode(CameraContract.View.MODE_RECORD_STOP);
        camera2Manager.pauseVideoRecord();
    }

    @Override
    public void restartRecord() {
        camera2Manager.takePictureOrVideo();
    }

    @Override
    public void setZoomFocus(float focusProportion) {
        camera2Manager.setZoomProportion(focusProportion);
    }

    @Override
    public void finishRecord() {
        mView.switchRecordMode(CameraContract.View.MODE_RECORD_FINISH);
        if (cycleTimeSubscription != null) {
            compositeSubscription.remove(cycleTimeSubscription);
        }
        time = 0;
    }

    @Override
    public void startRecord() {
        Log.d("camera_log","startRecord---------");
        mView.switchRecordMode(CameraContract.View.MODE_RECORD_START);
        cycleTimeSubscription = Observable.interval(1, TimeUnit.SECONDS, Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .unsubscribeOn(Schedulers.computation())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        time += 1000;
                        String time_show = TimingUtils.getDate(time);
                        Log.d("camera_log","startRecord---------="+time_show);
                        mView.setTimingShow(time_show);
                    }
                });
        compositeSubscription.add(cycleTimeSubscription);
    }

    /**
     * 返回camera mode
     * @return
     */
    @Override
    public int getCameraMode(){
        Log.d("camera_log","getCameraMode======"+currentMode);
        return currentMode;
    }
}
