package com.frank.glide.transformations;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.frank.glide.transformations.utils.GlideTransformationUtils;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

public class RoundedCornersTransformation extends BitmapTransformation {

    private static final String ID = "com.frank.glide.transformations.RoundedCornersTransformation";
    private static final byte[] ID_BYTES = ID.getBytes(CHARSET);

    public static final int ALL = 0;
    public static final int TOP_LEFT = 1;
    public static final int TOP_RIGHT = 2;
    public static final int BOTTOM_LEFT = 3;
    public static final int BOTTOM_RIGHT = 4;
    public static final int TOP = 5;
    public static final int BOTTOM = 6;
    public static final int LEFT = 7;
    public static final int RIGHT = 8;
    public static final int OTHER_TOP_LEFT = 9;
    public static final int OTHER_TOP_RIGHT = 10;
    public static final int OTHER_BOTTOM_LEFT = 11;
    public static final int OTHER_BOTTOM_RIGHT = 12;
    public static final int DIAGONAL_FROM_TOP_LEFT = 13;
    public static final int DIAGONAL_FROM_TOP_RIGHT = 14;

    private int mRadius;
    private int mMargin;
    private int mCornerType;

    public RoundedCornersTransformation(int radius, int margin) {
        this(radius, margin, RoundedCornersTransformation.ALL);
    }

    public RoundedCornersTransformation(int radius, int margin,
                                        int cornerType) {
        mRadius = radius;
        mMargin = margin;
        if (cornerType < 0 || cornerType > 14) {
            mCornerType = RoundedCornersTransformation.ALL;
        } else {
            mCornerType = cornerType;
        }
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        return GlideTransformationUtils.roundedCorners(pool, toTransform, outWidth, outHeight,
                mRadius, mMargin, mCornerType);
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

