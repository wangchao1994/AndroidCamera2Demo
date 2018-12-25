package com.example.wangchao.androidcamerabase;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.wangchao.androidcamerabase.base.BaseActivity;
import com.example.wangchao.androidcamerabase.base.BaseApplication;
import com.example.wangchao.androidcamerabase.camera.Camera2Manager;
import com.example.wangchao.androidcamerabase.contract.CameraContract;
import com.example.wangchao.androidcamerabase.imp.ICameraImp;
import com.example.wangchao.androidcamerabase.presenter.CameraPresenter;
import com.example.wangchao.androidcamerabase.utils.thread.WorkThreadUtils;
import com.example.wangchao.androidcamerabase.view.CameraFragment;

public class CameraActivity extends BaseActivity implements ICameraImp {
    public static final String TAG = CameraActivity.class.getSimpleName();
    private CameraFragment cameraFragment;
    private CameraContract.Presenter presenter;
    protected WorkThreadUtils mWorkThreadUtils;
    protected Camera2Manager mCamera2Manager;
    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }
    @Override
    protected void initView(Bundle savedInstanceState) {
        cameraFragment = (CameraFragment) getSupportFragmentManager().findFragmentByTag(CameraFragment.TAG);
        if (cameraFragment == null) {
            cameraFragment = CameraFragment.newInstance(this);
            getSupportFragmentManager().beginTransaction().add(R.id.main_content_layout, cameraFragment, CameraFragment.TAG).commitAllowingStateLoss();
        }
        presenter = new CameraPresenter(cameraFragment,this);
    }

    @Override
    protected void initManager() {
        mWorkThreadUtils = WorkThreadUtils.newInstance();
        mCamera2Manager = new Camera2Manager(BaseApplication.getInstance(),this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        presenter.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    @Override
    public WorkThreadUtils getWordThreadManger() {
        return mWorkThreadUtils;
    }

    @Override
    public Camera2Manager getCamera2Manager() {
        return mCamera2Manager;
    }

    @Override
    public void setFlashOpenOrClose(boolean values) {
        if (mCamera2Manager != null){
            mCamera2Manager.setFlashOnOrClose(values);
        }
    }
    @Override
    public boolean getFlashOpenOrClose() {
        if (mCamera2Manager != null){
            return  mCamera2Manager.getFlashOnOrClose();
        }
        return true;
    }

    @Override
    public void setZoomProportion(float values) {
        if (mCamera2Manager != null){
            mCamera2Manager.setZoomProportion(values);
        }
    }

    @Override
    public float getZoomProportion() {
        if (mCamera2Manager != null){
            return mCamera2Manager.getZoomProportion();
        }
        return 0;
    }

    @Override
    public void setManualFocus(boolean values) {
        if (mCamera2Manager != null){
            mCamera2Manager.setManualFocus(values);
        }
    }

    @Override
    public boolean getManualFocus() {
        if (mCamera2Manager != null){
            return mCamera2Manager.isManualFocus();
        }
        return false;
    }
    @Override
    public CameraContract.Presenter  getCameraModePresenter() {
        return presenter;
    }

}
