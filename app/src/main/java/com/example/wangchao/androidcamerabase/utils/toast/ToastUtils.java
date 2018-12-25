package com.example.wangchao.androidcamerabase.utils.toast;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;
/**
 * <p>
 * 一个Toast工具类
 */
public class ToastUtils {
    /**
     * 切换到UI线程显示Toast
     *
     * @param context
     * @param content
     */
    public static void showToastRunUIThread(final Context context, final String content) {
        if (context instanceof Activity){
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast(context, content);
                }
            });
        }
    }

    /**
     * 显示Toast
     * @param context
     * @param content
     */
    public static void showToast(Context context, String content) {
        Toast.makeText(context.getApplicationContext(), content, Toast.LENGTH_SHORT).show();
    }
}
