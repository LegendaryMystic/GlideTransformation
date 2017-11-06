package com.frank.glide.transformations;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

public class CropTransformation extends BitmapTransformation {

    private static final String ID = "com.frank.glide.transformations.CropTransformation";
    private static final byte[] ID_BYTES = ID.getBytes(CHARSET);

    private int mWidth;
    private int mHeight;
    private int mCropType;

    public CropTransformation(int width, int height) {
        this(width, height, GlideTransformationUtils.CROP_TYPE_CENTER);
    }

    public CropTransformation(int width, int height, @GlideTransformationUtils.CropType int cropType) {
        this.mWidth = width;
        this.mHeight = height;
        this.mCropType = cropType;
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        return GlideTransformationUtils.crop(pool, toTransform, mWidth, mHeight,
                mCropType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CropTransformation that = (CropTransformation) o;
        return mWidth == that.mWidth && mHeight == that.mHeight && mCropType == that.mCropType;
    }

    @Override
    public int hashCode() {
        int result = mWidth;
        result = 31 * result + mHeight;
        result = 31 * result + mCropType;
        return result;
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);
        int result = mWidth;
        result = 31 * result + mHeight;
        result = 31 * result + mCropType;
        byte[] radiusData = ByteBuffer.allocate(4).putInt(result).array();
        messageDigest.update(radiusData);
    }
}

