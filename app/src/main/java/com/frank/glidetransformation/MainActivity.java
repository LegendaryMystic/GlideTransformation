package com.frank.glidetransformation;

import android.app.Activity;
import android.content.Intent;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.frank.glide.transformations.BlurTransformation;
import com.frank.glide.transformations.CropTransformation;
import com.frank.glide.transformations.GlideTransformationUtils;
import com.frank.glide.transformations.RoundedCornersTransformation;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {


    private ImageView iv0_0;
    private ImageView iv0_1;
    private ImageView iv0_2;
    private ImageView iv1_0;
    private ImageView iv1_1;
    private ImageView iv1_2;
    private ImageView iv2_0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv0_0 = findViewById(R.id.iv0_0);
        iv0_1 = findViewById(R.id.iv0_1);
        iv0_2 = findViewById(R.id.iv0_2);
        iv1_0 = findViewById(R.id.iv1_0);
        iv1_1 = findViewById(R.id.iv1_1);
        iv1_2 = findViewById(R.id.iv1_2);
        iv2_0 = findViewById(R.id.iv2_0);
        Glide.with(this)
                .load("http://img.my.csdn.net/uploads/201508/05/1438760725_4031.jpg")
                .apply(new RequestOptions().transform(new BlurTransformation(this, 20, 5)))
                .into(iv0_0);
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter grayColorFilter = new ColorMatrixColorFilter(cm);
        iv0_1.setColorFilter(grayColorFilter);
        Glide.with(this)
                .load("http://img.my.csdn.net/uploads/201508/05/1438760725_4031.jpg")
                .into(iv0_1);
        iv0_2.setColorFilter(new PorterDuffColorFilter(0x80FF0000, PorterDuff.Mode.SRC_ATOP));
        Glide.with(this)
                .load("http://img.my.csdn.net/uploads/201508/05/1438760725_4031.jpg")
                .into(iv0_2);
        Glide.with(this)
                .load("http://img.my.csdn.net/uploads/201508/05/1438760725_4031.jpg")
                .apply(new RequestOptions().transform(new RoundedCornersTransformation(60, 0, GlideTransformationUtils.CORNER_TYPE_TOP)))
                .into(iv1_0);
        Glide.with(this)
                .load("http://img.my.csdn.net/uploads/201508/05/1438760725_4031.jpg")
                .apply(new RequestOptions().transform(new CircleCrop()))
                .into(iv1_1);
        Glide.with(this)
                .load("http://img.my.csdn.net/uploads/201508/05/1438760725_4031.jpg")
                .apply(new RequestOptions().transform(new CenterCrop()))
                .into(iv1_2);
        Glide.with(this)
                .load("http://img.my.csdn.net/uploads/201508/05/1438760725_4031.jpg")
                .apply(new RequestOptions().transform(new CropTransformation(50, 150)))
                .into(iv2_0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_all_cache:
                clearAllCache();
                break;
            case R.id.clear_memory_cache:
                clearMemoryCache();
                break;
            case R.id.clear_disk_cache:
                clearDiskCache();
                break;
            default:
                startActivity(new Intent(MainActivity.this, SecondActivity.class));
                break;
        }
        return true;
    }

    private void clearAllCache() {
        clearMemoryCache();
        clearDiskCache();
    }

    private void clearMemoryCache() {
        Glide.get(this).clearMemory();
    }

    private void clearDiskCache() {
        new ClearDiskCacheTask(this).execute();
    }

    private static class ClearDiskCacheTask extends AsyncTask<Void, Void, Void> {

        private final WeakReference<Activity> mActivity;

        public ClearDiskCacheTask(Activity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            final Activity activity = mActivity.get();
            if (activity != null) {
                Glide.get(activity).clearDiskCache();
            }
            return null;
        }
    }
}
