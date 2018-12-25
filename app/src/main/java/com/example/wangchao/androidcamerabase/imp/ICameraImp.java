package com.example.wangchao.androidcamerabase.imp;

import com.example.wangchao.androidcamerabase.camera.Camera2Manager;
import com.example.wangchao.androidcamerabase.contract.CameraContract;
import com.example.wangchao.androidcamerabase.utils.thread.WorkThreadUtils;

public interface ICameraImp {

    WorkThreadUtils getWordThreadManger();
    Camera2Manager getCamera2Manager();
    void setFlashOpenOrClose(boolean values);
    boolean getFlashOpenOrClose();
    void setZoomProportion(float values);
    float getZoomProportion();
    void setManualFocus(boolean values);
    boolean getManualFocus();
    CameraContract.Presenter getCameraModePresenter();

}
