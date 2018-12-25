package com.example.wangchao.androidcamerabase.camera;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.util.Log;
import android.view.TextureView;

import com.example.wangchao.androidcamerabase.camera.operate.PhotoMode;
import com.example.wangchao.androidcamerabase.camera.operate.VideoRecordOperator;
import com.example.wangchao.androidcamerabase.imp.ICameraImp;
import com.example.wangchao.androidcamerabase.mode.Constant;
import com.example.wangchao.androidcamerabase.utils.toast.ToastUtils;
/**
 * Camera管理类
 */
public class Camera2Manager {
    private static final String TAG = Camera2Manager.class.getSimpleName();
    private Context context;
    private BaseCamera2Operator pictureOperator;
    private BaseCamera2Operator videoRecordOperator;
    private BaseCamera2Operator currentOperator;
    private int currentDirection;
    private ICameraImp mICameraImp;
    private static final int CAMERA_BACK = 0;//默认使用后摄像头
    private static final int CAMERA_FRONT = 1;
    /**
     * 是否手动调焦
     */
    private boolean isManualFocus;
    /**
     * 焦距比例,手动调焦的模式下
     */
    private float zoomProportion;
    private boolean isFlashOnOrClose = true;
    public Camera2Manager(Context mContext, ICameraImp iCameraImp) {
        context = mContext;
        mICameraImp = iCameraImp;
        pictureOperator = new PhotoMode(mICameraImp);
        videoRecordOperator = new VideoRecordOperator(mICameraImp);
        setCurrentCameraDirection(currentDirection);
        //默认拍照模式
        currentOperator = pictureOperator;
        //是否开启手动调焦
        isManualFocus=false;
    }
    public void onResume(TextureView textureView) {
        videoRecordOperator.setWeakReference(textureView);
        pictureOperator.setWeakReference(textureView);
        currentOperator.startOperate();
    }

    public void onPause() {
        currentOperator.stopOperate();
    }

    public void setCamera2ResultCallBack(BaseCamera2Operator.Camera2ResultCallBack camera2ResultCallBack) {
        pictureOperator.setCamera2ResultCallBack(camera2ResultCallBack);
        videoRecordOperator.setCamera2ResultCallBack(camera2ResultCallBack);
    }
    public  void setCameraVideoCallBack(BaseCamera2Operator.Camera2VideoRecordCallBack cameraVideoCallBack){
        ((VideoRecordOperator) this.videoRecordOperator).setCamera2VideoRecordCallBack(cameraVideoCallBack);
    }

    public void takePictureOrVideo() {
        currentOperator.cameraClick();
    }
    /**
     * 给拍照和视频录制两种模式下设置，摄像头方向
     * @param currentDirection
     */
    private void setCurrentCameraDirection(int currentDirection){
        int direction=(currentDirection==CAMERA_BACK)? CameraCharacteristics.LENS_FACING_BACK:CameraCharacteristics.LENS_FACING_FRONT;
        Log.i("camera_log",TAG+" 切换摄像头为："+(direction== CameraCharacteristics.LENS_FACING_BACK?"后":"前"));
        //拍照和录像的操作类，记录当前的摄像头。
        videoRecordOperator.setCurrentDirection(direction);
        pictureOperator.setCurrentDirection(direction);
    }
    /**
     * 暂停视频
     */
    public  void pauseVideoRecord(){
        ((VideoRecordOperator) videoRecordOperator).pauseRecordingVideo();
    }
    /**
     * 切换到拍照还是录像模式
     * @param currentMode
     */
    public void switchMode(int currentMode) {
        Log.i(TAG,TAG+" CurrenMode： "+(currentMode==Constant.MODE_CAMERA?"Photo":"Video"));
        switch (currentMode) {
            //切换到拍照模式
            case Constant.MODE_CAMERA:
                videoRecordOperator.stopOperate();
                currentOperator = pictureOperator;
                break;
            //切换到录像模式
            case Constant.MODE_VIDEO_RECORD:
                pictureOperator.stopOperate();
                currentOperator = videoRecordOperator;
                break;
            default:
                break;
        }
        currentOperator.startOperate();
    }

    /**
     * 切换摄像头，前还是后
     * @param direction
     */
     public void switchCameraDirection(int direction){
         //相同摄像头方向，不进行操作
         if (currentDirection== direction){
             Log.d("camera_log","switchCameraDirection- currentDirection== direction---="+(currentDirection== direction));
             return;
         }
         //当视频录制状态，不能切换摄像头
         if (currentOperator instanceof  VideoRecordOperator){
             if (((VideoRecordOperator)currentOperator).isVideoRecord()){
                 ToastUtils.showToast(context,"请结束录像，再切换摄像头");
                 return;
             }
         }
         switch (direction){
             case  CAMERA_BACK:
                 ToastUtils.showToast(context,"请稍等，正在切换到后摄像头");
                 break;
             case  CAMERA_FRONT:
                 ToastUtils.showToast(context,"请稍等，正在切换到前摄像头");
                 break;
         }
         currentDirection = direction;
         Log.d("camera_log","switchCameraDirection----="+direction);
         setCurrentCameraDirection(currentDirection);
         currentOperator.switchCameraDirectionOperate();
     }

    /**
     * 设置焦距比例，从设置焦距值
     * @param mZoomProportion
     */
    public void setZoomProportion(float mZoomProportion) {
        zoomProportion = mZoomProportion;
        Log.d("camera_log","zoomProportion===="+zoomProportion);
        currentOperator.notifyFocusState();
    }

    /**
     * 获取缩放比例值
     * @return
     */
    public float getZoomProportion() {
        return zoomProportion;
    }
    /**
     * 是否手动对焦
     * @return
     */
    public boolean isManualFocus() {
        return isManualFocus;
    }
    /**
     * 设置是否手动调焦
     * @param manualFocus
     */
    public void setManualFocus(boolean manualFocus) {
        isManualFocus = manualFocus;
        currentOperator.notifyFocusState();
    }
    /**
     * 闪光灯开关
     * @param values
     */
    public void setFlashOnOrClose(boolean values){
        isFlashOnOrClose = values;
    }
    public boolean getFlashOnOrClose() {
        return isFlashOnOrClose;
    }
}
