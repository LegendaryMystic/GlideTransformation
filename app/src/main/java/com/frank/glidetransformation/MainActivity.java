package com.frank.glidetransformation;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.frank.glide.transformations.BlurTransformation;
import com.frank.glide.transformations.RoundedCornersTransformation;

public class MainActivity extends AppCompatActivity {

    private ImageView mImageView0;
    private ImageView mImageView1;
    private ImageView mImageView2;
    private ImageView mImageView3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView0 = (ImageView) findViewById(R.id.img0);
        mImageView1 = (ImageView) findViewById(R.id.img1);
        mImageView2 = (ImageView) findViewById(R.id.img2);
        mImageView3 = (ImageView) findViewById(R.id.img3);

        mImageView0.setImageResource(R.drawable.test001);

        RequestOptions options1 = new RequestOptions()
                .transform(new MultiTransformation<>(new CenterCrop(),
                        new RoundedCornersTransformation(20, 0, RoundedCornersTransformation.TOP)));
        Glide.with(this)
                .load("http://img.my.csdn.net/uploads/201508/05/1438760725_4031.jpg")
                .apply(options1)
                .into(mImageView1);

        RequestOptions options2 = new RequestOptions()
                .transform(new MultiTransformation<>(new CenterCrop(),
                        new BlurTransformation(this, 10, 5)));
        Glide.with(this)
                .load("http://img.my.csdn.net/uploads/201508/05/1438760725_4031.jpg")
                .apply(options2)
                .into(mImageView2);
    }
}
