package com.example.wangchao.androidcamerabase;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.wangchao.androidcamerabase.base.BaseActivity;

public class PictureShowActivity extends BaseActivity {
    public static final String TAG = PictureShowActivity.class.getSimpleName();
    @Override
    protected int getLayoutId() {
        return R.layout.activity_picture;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null & bundle.containsKey(TAG)) {
            String url = bundle.getString(TAG);
            ImageView imageView = (ImageView) findViewById(R.id.picture_show_iv);
            Glide.with(this).asBitmap().load(url).into(imageView);
        }
    }
    public static void openActivity(Context context, String url) {
        Bundle bundle = new Bundle();
        bundle.putString(PictureShowActivity.TAG, url);
        Intent intent = new Intent(context, PictureShowActivity.class);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }
    @Override
    protected void initManager() {
    }
}
