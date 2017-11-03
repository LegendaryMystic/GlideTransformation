package com.frank.glide.transformations;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

public class RoundedCornersTransformation extends BitmapTransformation {

    private static final String ID = "com.frank.glide.transformations.RoundedCornersTransformation";
    private static final byte[] ID_BYTES = ID.getBytes(CHARSET);

    private int mRadius;
    private int mMargin;
    private int mCornerType;

    public RoundedCornersTransformation(int radius, int margin) {
        this(radius, margin, GlideTransformationUtils.ALL);
    }

    public RoundedCornersTransformation(int radius, int margin,
                                        int cornerType) {
        mRadius = radius;
        mMargin = margin;
        if (cornerType < 0 || cornerType > 14) {
            mCornerType = GlideTransformationUtils.ALL;
        } else {
            mCornerType = cornerType;
        }
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        return GlideTransformationUtils.roundedCorners(pool, toTransform, mRadius, mMargin, mCornerType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RoundedCornersTransformation that = (RoundedCornersTransformation) o;

        if (mRadius != that.mRadius) return false;
        if (mMargin != that.mMargin) return false;
        return mCornerType == that.mCornerType;

    }

    @Override
    public int hashCode() {
        int result = mRadius;
        result = 31 * result + mMargin;
        result = 31 * result + mCornerType;
        return ID.hashCode() + result;
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);
        int result = mRadius;
        result = 31 * result + mMargin;
        result = 31 * result + mCornerType;
        byte[] radiusData = ByteBuffer.allocate(4).putInt(result).array();
        messageDigest.update(radiusData);
    }

}

