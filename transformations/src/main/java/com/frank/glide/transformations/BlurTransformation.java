package com.frank.glide.transformations;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.frank.glide.transformations.utils.GlideTransformationUtils;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

public class BlurTransformation extends BitmapTransformation {

    private static final String ID = "com.frank.glide.transformations.BlurTransformation";
    private static final byte[] ID_BYTES = ID.getBytes(CHARSET);

    private Context mContext;
    private int mRadius;
    private int mSampling;

    public BlurTransformation(Context context) {
        this(context, 20);
    }

    public BlurTransformation(Context context, int radius) {
        this(context, radius, 1);
    }

    public BlurTransformation(Context context, int radius, int sampling) {
        mContext = context;
        mRadius = radius;
        mSampling = sampling;
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        return GlideTransformationUtils.blur(pool, toTransform, outWidth, outHeight,
                mContext, mRadius, mSampling);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BlurTransformation that = (BlurTransformation) o;

        if (mRadius != that.mRadius) return false;
        return mSampling == that.mSampling;

    }

    @Override
    public int hashCode() {
        int result = mRadius;
        result = 31 * result + mSampling;
        return ID.hashCode() + result;
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);
        int result = mRadius;
        result = 31 * result + mSampling;
        byte[] radiusData = ByteBuffer.allocate(4).putInt(result).array();
        messageDigest.update(radiusData);
    }

}
