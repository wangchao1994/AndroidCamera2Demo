package com.example.wangchao.androidcamerabase.contract;

import android.view.TextureView;

public interface CameraContract {

    interface Presenter {
        /**
         * 与View的onResume()生命周期保持一致
         */
        void onResume();
        /**
         * 与View的onPause()生命周期保持一致
         */
        void onPause();
        /**
         * 权限请求结果
         *
         * @param requestCode
         * @param permissions
         * @param grantResults
         */
        void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults);

        /**
         * 拍照或者录像
         */
        void takePictureOrVideo();

        /**
         * 切换模式： 拍照，录像
         * @param mode
         */
        void switchMode(int mode);

        /**
         * 切换摄像头，包含前后两种
         * @param direction
         */
        void switchCamera(int direction);

        /**
         * 暂停录制
         */
        void stopRecord();

        /**
         * 重新开始录制
         */
        void restartRecord();

        /**
         * 设置手动调焦的比例值
         * @param focusProportion
         */
        void setZoomFocus(float focusProportion);

        /**
         * 返回当前模式(拍照或是录像)
         * @return
         */
        int getCameraMode();
    }

    interface View<T extends Presenter> {
        /**
         * 获取TextureView
         * @return
         */
        TextureView getCameraView();
        /**
         * 加载拍照的图片路径
         *
         * @param filePath
         */
        void loadPictureResult(String filePath);

        /**
         * 显示计时时间
         * @param timing
         */
        void setTimingShow(String timing);

        /**
         *切换到录制状态
         * @param  mode
         *
         */
        void switchRecordMode(int mode);
        /**
         * toast提示
         * @param content
         */
        void  showToast(String content);
        /**
         * 视频录制的三种状态,开始，停止，完成
         */
        int MODE_RECORD_START=1;
        int MODE_RECORD_STOP=2;
        int MODE_RECORD_FINISH=3;

    }
}
