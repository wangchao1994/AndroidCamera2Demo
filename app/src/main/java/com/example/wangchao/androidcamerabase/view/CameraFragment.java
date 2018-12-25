package com.example.wangchao.androidcamerabase.view;

import android.animation.Animator;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.wangchao.androidcamerabase.PictureShowActivity;
import com.example.wangchao.androidcamerabase.R;
import com.example.wangchao.androidcamerabase.contract.CameraContract;
import com.example.wangchao.androidcamerabase.glide.GlideLoader;
import com.example.wangchao.androidcamerabase.imp.ICameraImp;
import com.example.wangchao.androidcamerabase.mode.Constant;
import com.example.wangchao.androidcamerabase.utils.animator.AnimatorBuilder;
import com.example.wangchao.androidcamerabase.utils.toast.ToastUtils;
import com.example.wangchao.androidcamerabase.widget.AutoFitTextureView;
/**
 * CameraFragment
 */
public class CameraFragment extends Fragment implements CameraContract.View<CameraContract.Presenter> , View.OnClickListener{
    public static final String TAG = CameraFragment.class.getSimpleName();
    private ImageView show_result_iv;
    private ImageView controller_state_iv;
    private Animator flashAnimator;
    private AutoFitTextureView textureView;
    private TextView show_record_tv, record_tip_circle;
    private View rootView;
    private CameraContract.Presenter presenter;
    protected String filePath;
    private ImageView mCameraPhoto;
    private ImageView mCameraflash;
    private ImageView mCameraSwitch;
    private ImageView mCameraZoomChange;
    private static ICameraImp mICameraImp;
    //video/photo mode
    private TextView tv_camera_photo;
    private TextView tv_camera_video;
    boolean isCameraBack = true;
    public static CameraFragment newInstance(ICameraImp iCameraImp) {
        mICameraImp = iCameraImp;
        return new CameraFragment();
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter = mICameraImp.getCameraModePresenter();
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_camera, container, false);
        initView();
        return rootView;
    }

    @Override
    public void showToast(String content) {
        ToastUtils.showToastRunUIThread(getActivity(),content);
    }

    private void initView() {
        textureView = rootView.findViewById(R.id.camera_auto_fit_texture_view);
        show_result_iv = rootView.findViewById(R.id.camera_show);
        show_record_tv = rootView.findViewById(R.id.camera_video_record_tip_time_tv);
        record_tip_circle = rootView.findViewById(R.id.camera_video_record_tip_bg);
        mCameraPhoto = rootView.findViewById(R.id.camera_btn);
        controller_state_iv = rootView.findViewById(R.id.camera_right_top_controller);
        mCameraflash = rootView.findViewById(R.id.camera_flash);
        mCameraSwitch = rootView.findViewById(R.id.camera_switch);
        mCameraZoomChange = rootView.findViewById(R.id.camera_zoom_change);
        tv_camera_photo = rootView.findViewById(R.id.tv_camera_photo);
        tv_camera_video = rootView.findViewById(R.id.tv_camera_video);
        //录制状态TAG
        controller_state_iv.setTag(CameraContract.View.MODE_RECORD_FINISH);
        mCameraPhoto.setOnClickListener(this);
        controller_state_iv.setOnClickListener(this);
        show_result_iv.setOnClickListener(this);
        mCameraflash.setOnClickListener(this);
        mCameraSwitch.setOnClickListener(this);
        mCameraZoomChange.setOnClickListener(this);
        tv_camera_photo.setOnClickListener(this);
        tv_camera_video.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (presenter != null) {
            presenter.onResume();
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        if (presenter != null) {
            presenter.onPause();
        }
    }

    @Override
    public TextureView getCameraView() {
        return textureView;
    }

    @Override
    public void loadPictureResult(String mFilePath) {
        filePath = mFilePath;
        GlideLoader.loadNetWorkResource(getActivity(), filePath, show_result_iv);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //点击按钮，进行拍照或录像
            case R.id.camera_btn:
                int cameraMode = presenter.getCameraMode();
                if (cameraMode == 1){
                    Log.d("camera_log","cameraMode PhotoMode====================="+cameraMode);
                    controller_state_iv.setVisibility(View.GONE);
                }else if (cameraMode == 2){
                    controller_state_iv.setVisibility(View.VISIBLE);
                    Log.d("camera_log","cameraMode VideoMode====================="+cameraMode);
                }
                presenter.takePictureOrVideo();
                break;
            // 控制视频录制的开关,包含暂停，恢复录制
            case R.id.camera_right_top_controller:
                int mode = (int) controller_state_iv.getTag();
                if (mode == CameraContract.View.MODE_RECORD_START) { //录制状态中，可以暂停
                    controller_state_iv.setImageResource(R.drawable.ic_recording_play);
                    presenter.stopRecord();
                }else if (mode == CameraContract.View.MODE_RECORD_STOP) {//暂停状态，可以继续开始录制
                    controller_state_iv.setImageResource(R.drawable.ic_recording_pause);
                    presenter.restartRecord();
                }
                break;
            case R.id.camera_show:
                if (!TextUtils.isEmpty(filePath)) {
                   PictureShowActivity.openActivity(getActivity(), filePath);
                }
                break;
            case R.id.camera_flash:
                boolean flashOpenOrClose = mICameraImp.getFlashOpenOrClose();
                Log.d("camera_log","flashOpenOrClose  mICameraImp.getFlashOpenOrClose()====="+mICameraImp.getFlashOpenOrClose());
                if (flashOpenOrClose){
                    mICameraImp.setFlashOpenOrClose(false);
                    mCameraflash.setImageResource(R.drawable.btn_flash_off_normal);
                }else{
                    mICameraImp.setFlashOpenOrClose(true);
                    mCameraflash.setImageResource(R.drawable.btn_flash_auto_normal);
                }
                break;
            case R.id.camera_switch:
                if (isCameraBack){
                    presenter.switchCamera(1);
                    isCameraBack = false;
                    Log.d("camera_log","------------------CAMERA_FACING_FRONT isCameraBack="+isCameraBack);
                }else{
                    presenter.switchCamera(0);
                    isCameraBack = true;
                    Log.d("camera_log","------------------CAMERA_FACING_BACK isCameraBack="+isCameraBack);
                }
                break;
            case R.id.camera_zoom_change:
                float zoomProportion = mICameraImp.getZoomProportion();
                presenter.setZoomFocus(4.0f);
                Log.d("camera_log","zoomProportion==camera_zoom_change==="+zoomProportion);
                break;
             //video or photo Mode
            case R.id.tv_camera_photo:
                presenter.switchMode(Constant.MODE_CAMERA);
                mCameraPhoto.setImageResource(R.drawable.main_camera_photo);
                tv_camera_photo.setTextColor(Color.RED);
                tv_camera_video.setTextColor(Color.WHITE);
                break;
            case R.id.tv_camera_video:
                presenter.switchMode(Constant.MODE_VIDEO_RECORD);
                mCameraPhoto.setImageResource(R.drawable.main_camera_video);
                tv_camera_photo.setTextColor(Color.WHITE);
                tv_camera_video.setTextColor(Color.RED);
                break;
            default:
                break;
        }
    }

    @Override
    public void setTimingShow(String timing) {
        show_record_tv.setText(timing);
    }

    @Override
    public void switchRecordMode(int mode) {
        switch (mode) {
            //录制开始
            case CameraContract.View.MODE_RECORD_START:
                show_record_tv.setVisibility(View.VISIBLE);
                record_tip_circle.setVisibility(View.VISIBLE);
                if (flashAnimator != null && flashAnimator.isRunning()) {
                    flashAnimator.cancel();
                }
                showViewMainUI(false);
                break;
            //录制暂停
            case CameraContract.View.MODE_RECORD_STOP:
                show_record_tv.setVisibility(View.INVISIBLE);
                flashAnimator = AnimatorBuilder.createFlashAnimator(record_tip_circle);
                flashAnimator.start();
                break;
            //录制完成
            case CameraContract.View.MODE_RECORD_FINISH:
                show_record_tv.setText("");
                show_record_tv.setVisibility(View.GONE);
                record_tip_circle.setVisibility(View.GONE);
                controller_state_iv.setVisibility(View.GONE);
                showViewMainUI(true);
                break;
            default:
                break;
        }
        controller_state_iv.setTag(mode);
    }


    /***
     *视频录制UI显示与隐藏
     */
    public void showViewMainUI(boolean isShow){
        if (isShow){
            tv_camera_photo.setVisibility(View.VISIBLE);
            tv_camera_video.setVisibility(View.VISIBLE);
            mCameraZoomChange.setVisibility(View.VISIBLE);
            mCameraflash.setVisibility(View.VISIBLE);
            mCameraSwitch.setVisibility(View.VISIBLE);
            mCameraSwitch.setVisibility(View.VISIBLE);
        }else{
            tv_camera_photo.setVisibility(View.GONE);
            tv_camera_video.setVisibility(View.GONE);
            mCameraZoomChange.setVisibility(View.GONE);
            mCameraflash.setVisibility(View.GONE);
            mCameraSwitch.setVisibility(View.GONE);
            mCameraSwitch.setVisibility(View.GONE);
        }
    }

}
