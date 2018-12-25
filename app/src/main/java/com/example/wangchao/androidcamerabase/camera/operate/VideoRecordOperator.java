package com.example.wangchao.androidcamerabase.camera.operate;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import com.example.wangchao.androidcamerabase.camera.BaseCamera2Operator;
import com.example.wangchao.androidcamerabase.camera.Camera2Manager;
import com.example.wangchao.androidcamerabase.camera.utils.Camera2Utils;
import com.example.wangchao.androidcamerabase.imp.ICameraImp;
import com.example.wangchao.androidcamerabase.utils.File.FileUtils;
import com.example.wangchao.androidcamerabase.utils.permission.PermissionsManager;
import com.example.wangchao.androidcamerabase.utils.rxjava.ObservableBuilder;
import com.example.wangchao.androidcamerabase.utils.thread.WorkThreadUtils;
import com.example.wangchao.androidcamerabase.utils.toast.ToastUtils;
import com.example.wangchao.androidcamerabase.widget.AutoFitTextureView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * <p>
 * 录像操作类
 */

public class VideoRecordOperator extends BaseCamera2Operator {
    public static final String TAG = VideoRecordOperator.class.getSimpleName();
    private WorkThreadUtils workThreadManager;
    private List<String> oldVideoPath;
    /**
     * 视频录制的大小
     */
    private Size mVideoSize;
    /**
     * 相机预览的大小Size
     */
    private Size mPreviewSize;
    /**
     * MediaRecorder
     */
    private MediaRecorder mMediaRecorder;
    /**
     * 当前是否是在录制视频
     */
    private boolean mIsRecordingVideo;
    /**
     * 传感器的方向
     */
    private Integer mSensorOrientation;
    /**
     * 相机预览请求的Builder
     */
    private CaptureRequest.Builder mPreviewBuilder;
    /**
     * 点击开启录制时候创建的新视频文件路径
     */
    private String mNextVideoAbsolutePath;
    private CompositeSubscription compositeSubscription;
    private Rect zoomRect;
    private ICameraImp mICameraImp;
    private float maxZoom;
    private CameraCharacteristics characteristics;
    private boolean isRecordGonging = false;

    public VideoRecordOperator(ICameraImp iCameraImp) {
        mICameraImp = iCameraImp;
        workThreadManager = mICameraImp.getWordThreadManger();
        oldVideoPath = new CopyOnWriteArrayList<>();
        compositeSubscription = new CompositeSubscription();
    }
    @Override
    public void writePictureData(Image image) {
    }

    /**
     * 开始相机的预览界面，创建一个预览的session会话。
     */
    @Override
    public void startPreView() {
        TextureView mTextureView = getTextureView();
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        //开始相机预览
        try {
            closePreviewSession();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            Surface previewSurface = new Surface(texture);
            mPreviewBuilder.addTarget(previewSurface);
            //从拍照切换到摄像头，获取录像完成后需要重新恢复以前的状态
            float currentZoom = mICameraImp.getCamera2Manager().getZoomProportion() * maxZoom;
            updateZoomRect(currentZoom);
            mCameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mPreviewSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Activity activity = getTextureViewContext();
                            if (null != activity) {
                                Toast.makeText(activity, "相机预览配置失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, workThreadManager.getBackgroundHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        if (null != mTextureView) {
            configureTransform(getTextureViewContext(), mTextureView.getWidth(), mTextureView.getHeight());
        }
    }

    /**
     * 在 startPreView()之后执行用于更新相机预览界面
     */
    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder);
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, workThreadManager.getBackgroundHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }


    /**
     * 为相机创建一个CameraCaptureSession
     */
    private CameraCaptureSession mPreviewSession;

    @Override
    public void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }
    @Override
    public void cameraClick() {
        if (mIsRecordingVideo) {
            stopRecordingVideo(true);
        } else {
            startRecordingVideo();
        }
    }
    /**
     * 停止录制
     */
    private void stopRecordingVideo(final boolean isFinish) {
        mIsRecordingVideo = false;
        /**
         * 在MediaRecorder停止前，停止相机预览，防止抛出serious error异常。
         * android.hardware.camera2.CameraAccessException: The camera device has encountered a serious error
         * 解决方式：https://stackoverflow.com/questions/27907090/android-camera-2-api
         */
        try {
            mPreviewSession.stopRepeating();
            mPreviewSession.abortCaptures();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Subscription subscription = Observable
                //延迟三十毫秒
                .timer(30, TimeUnit.MICROSECONDS, Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        // 停止录制
                        mMediaRecorder.stop();
                        mMediaRecorder.reset();
                        if (isFinish) {
                            isRecordGonging = false;
                            Log.i(TAG, "stopRecordingVideo recording complete--------");
                            if (camera2VideoRecordCallBack != null) {
                                camera2VideoRecordCallBack.finishRecord();
                            }
                            mergeMultipleFileCallBack();
                            mNextVideoAbsolutePath = null;
                            oldVideoPath.clear();
                        } else {//暂停的操作
                            Log.i(TAG, "pauseRecordingVideo recording stop--------");
                            //若是开始新的录制，原本暂停产生的多个文件合并成一个文件。
                            oldVideoPath.add(mNextVideoAbsolutePath);
                            if (oldVideoPath.size() > 1) {
                                mergeMultipleFile();
                            }
                            mNextVideoAbsolutePath = null;
                        }
                        startPreView();
                    }
                });
                compositeSubscription.add(subscription);
    }

    /**
     * 暂停后又从新恢复录制，合并多个视频文件
     */
    private void mergeMultipleFile() {
        Log.i(TAG, " mergeMultipleFile  开始操作：文件个数 " + oldVideoPath.size());
        Subscription subscription = ObservableBuilder.createMergeMuiltFile(appContext, oldVideoPath.get(0), oldVideoPath.get(1))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String filePath) {
                        oldVideoPath.clear();
                        oldVideoPath.add(filePath);
                        Log.i(TAG, " mergeMultipleFile  完成： 文件个数" + oldVideoPath.size());
                    }
                });

        compositeSubscription.add(subscription);
    }

    /**
     * 完成录制，输出最终的视频录制文件
     */
    private void mergeMultipleFileCallBack() {
        if (this.oldVideoPath.size() > 0) {
            Log.i(TAG, " mergeMultipleFileCallBack file.size()===" + oldVideoPath.size());
            Subscription subscription = ObservableBuilder.createMergeMuiltFile(appContext, this.oldVideoPath.get(0), mNextVideoAbsolutePath)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            if (mCamera2ResultCallBack != null) {
                                mCamera2ResultCallBack.callBack(ObservableBuilder.createVideo(s));
                            }
                            Log.i(TAG, " mergeMultipleFileCallBack--------success-------------");
                            ToastUtils.showToast(appContext, "视频文件保存路径:" + s);
                        }
                    });
                    compositeSubscription.add(subscription);
        } else {
            if (mCamera2ResultCallBack != null) {
                mCamera2ResultCallBack.callBack(ObservableBuilder.createVideo(mNextVideoAbsolutePath));
            }
            ToastUtils.showToast(appContext, "视频文件保存在" + mNextVideoAbsolutePath);
        }
    }

    /**
     * 暂停录制
     */
    public void pauseRecordingVideo() {
        stopRecordingVideo(false);
    }

    /**
     * 开始视频录制，创建一个录像的session会话。
     */
    private void startRecordingVideo() {

        Log.i(TAG, " startRecordingVideo  录制初始化 ");
        TextureView mTextureView = getTextureView();
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }

        try {
            closePreviewSession();
            setUpMediaRecorder();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            //创建录制的session会话
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();
            // 为相机预览设置Surface
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);
            // 为 MediaRecorder设置Surface
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);
            //与未录像的状态保持一致。
            if (zoomRect != null) {
                mPreviewBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
            }
            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    updatePreview();
                    Log.i(TAG, " startRecordingVideo  isRecording----------------- ");
                    getTextureViewContext().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mIsRecordingVideo = true;
                            isRecordGonging = true;
                            mMediaRecorder.start();
                            if (camera2VideoRecordCallBack != null) {
                                camera2VideoRecordCallBack.startRecord();
                            }
                        }
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Activity activity = getTextureViewContext();
                    if (null != activity) {
                        Toast.makeText(activity.getApplicationContext(), "相机设备配置失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }, workThreadManager.getBackgroundHandler());
        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 设置媒体录制器的配置参数
     * <p>
     * 音频，视频格式，文件路径，频率，编码格式等等
     *
     * @throws IOException
     */
    private void setUpMediaRecorder() throws IOException {
        final Activity activity = getTextureViewContext();
        if (null == activity) {
            return;
        }
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mNextVideoAbsolutePath = FileUtils.createVideoDiskFile(appContext, FileUtils.createVideoFileName()).getAbsolutePath();
        mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        //每秒30帧
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (mSensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                mMediaRecorder.setOrientationHint(ORIENTATIONS.get(rotation));
                break;
            default:
                break;
        }
        mMediaRecorder.prepare();
    }



    @Override
    public void startOperate() {
        TextureView textureView = getTextureView();
        if (textureView.isAvailable()) {
            openCamera(getTextureViewContext(), textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void stopOperate() {
        try {
            mCameraOpenCloseLock.acquire();
            closePreviewSession();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mMediaRecorder) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }

        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            mCameraOpenCloseLock.release();
        }
    }
    /**
     * 是否在进行视频录制，录制状态，包含进行中，暂停中。
     *
     * @return
     */
    public boolean isVideoRecord() {
        return isRecordGonging;
    }
    /**
     * 更新缩放，数字调焦
     *
     * @param currentZoom
     */
    private void updateZoomRect(float currentZoom) {
        try {
            Rect rect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            if (rect == null) {
                return;
            }
            zoomRect = Camera2Utils.createZoomRect(rect, currentZoom);
            if (zoomRect == null) {
                return;
            }
            Log.i(TAG, "zoom对应的 rect对应的区域 " + zoomRect.left + " " + zoomRect.right + " " + zoomRect.top + " " + zoomRect.bottom);
            mPreviewBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Override
    protected void openCamera(Activity activity, int width, int height) {
        if (PermissionsManager.checkVideoRecordPermission(getTextureViewContext())) {
            if (null == activity || activity.isFinishing()) {
                return;
            }
            AutoFitTextureView textureView = (AutoFitTextureView) getTextureView();
            CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            try {
                Log.d(TAG, "tryAcquire");
                if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("锁住相机开启，超时");
                }
                for (String cameraId : manager.getCameraIdList()) {
                    characteristics = manager.getCameraCharacteristics(cameraId);
                    if (!Camera2Utils.matchCameraDirection(characteristics, mCurrentDirection)) {
                        continue;
                    }
                    //存储流配置类
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (map == null) {
                        continue;
                    }
                    Log.i(TAG, "视频录制，重新配置相机设备" + mCurrentDirection + " " + cameraId);
                    mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    Float maxZoomValue = Camera2Utils.getMaxZoom(characteristics);
                    if (maxZoomValue != null) {
                        maxZoom = maxZoomValue;
                    }
                    // 计算相机预览和视频录制的的Size
                    mVideoSize = Camera2Utils.chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
                    mPreviewSize = Camera2Utils.chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, mVideoSize);
                    int orientation = activity.getResources().getConfiguration().orientation;
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        textureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                    } else {
                        textureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
                    }
                    configureTransform(activity, width, height);
                    mMediaRecorder = new MediaRecorder();
                    manager.openCamera(cameraId, stateCallback, null);
                    return;
                }
            } catch (CameraAccessException e) {
                ToastUtils.showToast(appContext, "不能访问相机");
                activity.finish();
            } catch (NullPointerException e) {
                ToastUtils.showToast(appContext, "当前设备不支持Camera2 API");
            } catch (InterruptedException e) {
                throw new RuntimeException("在锁住相机开启期间被打断.");
            }
        }
    }

    @Override
    protected void configureTransform(Activity activity, int viewWidth, int viewHeight) {
        TextureView textureView = getTextureView();
        if (null == textureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) viewHeight / mPreviewSize.getHeight(), (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    @Override
    public void notifyFocusState() {
        float currentZoom = mICameraImp.getCamera2Manager().getZoomProportion() * maxZoom;
        updateZoomRect(currentZoom);
        updatePreview();
    }

    private Camera2VideoRecordCallBack camera2VideoRecordCallBack;
    public void setCamera2VideoRecordCallBack(Camera2VideoRecordCallBack mCamera2VideoRecordCallBack) {
        camera2VideoRecordCallBack = mCamera2VideoRecordCallBack;
    }

    @Override
    public void setPreviewZoomValues(float values) {
    }
}
