package com.frank.glide.transformations.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.support.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Synthetic;
import com.frank.glide.transformations.RoundedCornersTransformation;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GlideTransformationUtils {

    public static final int PAINT_FLAGS = Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG;
    private static final Paint DEFAULT_PAINT = new Paint(PAINT_FLAGS);

    // See #738.
    private static final List<String> MODELS_REQUIRING_BITMAP_LOCK =
            Arrays.asList(
                    "XT1097",
                    "XT1085");
    /**
     * https://github.com/bumptech/glide/issues/738 On some devices (Moto X with android 5.1) bitmap
     * drawing is not thread safe.
     * This lock only locks for these specific devices. For other types of devices the lock is always
     * available and therefore does not impact performance
     */
    private static final Lock BITMAP_DRAWABLE_LOCK =
            MODELS_REQUIRING_BITMAP_LOCK.contains(Build.MODEL)
                    && Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1
                    ? new ReentrantLock() : new NoLock();

    private GlideTransformationUtils() {
        // Utility class.
    }


    public static Lock getBitmapDrawableLock() {
        return BITMAP_DRAWABLE_LOCK;
    }

    public static Bitmap roundedCorners(@NonNull BitmapPool pool, @NonNull Bitmap inBitmap,
                                        int width, int height,
                                        int radius, int margin,
                                        int cornerType) {
        Preconditions.checkArgument(width > 0, "width must be greater than 0.");
        Preconditions.checkArgument(height > 0, "height must be greater than 0.");

        // Alpha is required for this transformation.
        Bitmap toTransform = getAlphaSafeBitmap(pool, inBitmap);
        Bitmap result = pool.get(width, height, Bitmap.Config.ARGB_8888);

        result.setHasAlpha(true);

        BitmapShader shader = new BitmapShader(toTransform, Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(shader);
        BITMAP_DRAWABLE_LOCK.lock();
        try {
            Canvas canvas = new Canvas(result);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            float right = width - margin;
            float bottom = height - margin;
            int diameter = radius * 2;

            switch (cornerType) {
                case RoundedCornersTransformation.ALL:
                    canvas.drawRoundRect(new RectF(margin, margin, right, bottom), radius, radius, paint);
                    break;
                case RoundedCornersTransformation.TOP_LEFT:
                    canvas.drawRoundRect(new RectF(margin, margin, margin + diameter, margin + diameter),
                            radius, radius, paint);
                    canvas.drawRect(new RectF(margin, margin + radius, margin + radius, bottom), paint);
                    canvas.drawRect(new RectF(margin + radius, margin, right, bottom), paint);
                    break;
                case RoundedCornersTransformation.TOP_RIGHT:
                    canvas.drawRoundRect(new RectF(right - diameter, margin, right, margin + diameter), radius,
                            radius, paint);
                    canvas.drawRect(new RectF(margin, margin, right - radius, bottom), paint);
                    canvas.drawRect(new RectF(right - radius, margin + radius, right, bottom), paint);
                    break;
                case RoundedCornersTransformation.BOTTOM_LEFT:
                    canvas.drawRoundRect(new RectF(margin, bottom - diameter, margin + diameter, bottom),
                            radius, radius, paint);
                    canvas.drawRect(new RectF(margin, margin, margin + diameter, bottom - radius), paint);
                    canvas.drawRect(new RectF(margin + radius, margin, right, bottom), paint);
                    break;
                case RoundedCornersTransformation.BOTTOM_RIGHT:
                    canvas.drawRoundRect(new RectF(right - diameter, bottom - diameter, right, bottom), radius,
                            radius, paint);
                    canvas.drawRect(new RectF(margin, margin, right - radius, bottom), paint);
                    canvas.drawRect(new RectF(right - radius, margin, right, bottom - radius), paint);
                    break;
                case RoundedCornersTransformation.TOP:
                    canvas.drawRoundRect(new RectF(margin, margin, right, margin + diameter), radius, radius,
                            paint);
                    canvas.drawRect(new RectF(margin, margin + radius, right, bottom), paint);
                    break;
                case RoundedCornersTransformation.BOTTOM:
                    canvas.drawRoundRect(new RectF(margin, bottom - diameter, right, bottom), radius, radius,
                            paint);
                    canvas.drawRect(new RectF(margin, margin, right, bottom - radius), paint);
                    break;
                case RoundedCornersTransformation.LEFT:
                    canvas.drawRoundRect(new RectF(margin, margin, margin + diameter, bottom), radius, radius,
                            paint);
                    canvas.drawRect(new RectF(margin + radius, margin, right, bottom), paint);
                    break;
                case RoundedCornersTransformation.RIGHT:
                    canvas.drawRoundRect(new RectF(right - diameter, margin, right, bottom), radius, radius,
                            paint);
                    canvas.drawRect(new RectF(margin, margin, right - radius, bottom), paint);
                    break;
                case RoundedCornersTransformation.OTHER_TOP_LEFT:
                    canvas.drawRoundRect(new RectF(margin, bottom - diameter, right, bottom), radius, radius,
                            paint);
                    canvas.drawRoundRect(new RectF(right - diameter, margin, right, bottom), radius, radius,
                            paint);
                    canvas.drawRect(new RectF(margin, margin, right - radius, bottom - radius), paint);
                    break;
                case RoundedCornersTransformation.OTHER_TOP_RIGHT:
                    canvas.drawRoundRect(new RectF(margin, margin, margin + diameter, bottom), radius, radius,
                            paint);
                    canvas.drawRoundRect(new RectF(margin, bottom - diameter, right, bottom), radius, radius,
                            paint);
                    canvas.drawRect(new RectF(margin + radius, margin, right, bottom - radius), paint);
                    break;
                case RoundedCornersTransformation.OTHER_BOTTOM_LEFT:
                    canvas.drawRoundRect(new RectF(margin, margin, right, margin + diameter), radius, radius,
                            paint);
                    canvas.drawRoundRect(new RectF(right - diameter, margin, right, bottom), radius, radius,
                            paint);
                    canvas.drawRect(new RectF(margin, margin + radius, right - radius, bottom), paint);
                    break;
                case RoundedCornersTransformation.OTHER_BOTTOM_RIGHT:
                    canvas.drawRoundRect(new RectF(margin, margin, right, margin + diameter), radius, radius,
                            paint);
                    canvas.drawRoundRect(new RectF(margin, margin, margin + diameter, bottom), radius, radius,
                            paint);
                    canvas.drawRect(new RectF(margin + radius, margin + radius, right, bottom), paint);
                    break;
                case RoundedCornersTransformation.DIAGONAL_FROM_TOP_LEFT:
                    canvas.drawRoundRect(new RectF(margin, margin, margin + diameter, margin + diameter),
                            radius, radius, paint);
                    canvas.drawRoundRect(new RectF(right - diameter, bottom - diameter, right, bottom), radius,
                            radius, paint);
                    canvas.drawRect(new RectF(margin, margin + radius, right - diameter, bottom), paint);
                    canvas.drawRect(new RectF(margin + diameter, margin, right, bottom - radius), paint);
                    break;
                case RoundedCornersTransformation.DIAGONAL_FROM_TOP_RIGHT:
                    canvas.drawRoundRect(new RectF(right - diameter, margin, right, margin + diameter), radius,
                            radius, paint);
                    canvas.drawRoundRect(new RectF(margin, bottom - diameter, margin + diameter, bottom),
                            radius, radius, paint);
                    canvas.drawRect(new RectF(margin, margin, right - radius, bottom - radius), paint);
                    canvas.drawRect(new RectF(margin + radius, margin + radius, right, bottom), paint);
                    break;
                default:
                    canvas.drawRoundRect(new RectF(margin, margin, right, bottom), radius, radius, paint);
                    break;
            }
            clear(canvas);
        } finally {
            BITMAP_DRAWABLE_LOCK.unlock();
        }

        if (!toTransform.equals(inBitmap)) {
            pool.put(toTransform);
        }

        return result;
    }

    public static Bitmap blur(@NonNull BitmapPool pool, @NonNull Bitmap inBitmap, int width, int height,
                              Context context, int radius, int sampling) {
        Preconditions.checkArgument(radius > 0 && radius < 26,
                "radius must be greater than 0 and less than 26.");
        Preconditions.checkArgument(sampling > 0,
                "sampling must be greater than 0.");

        int srcWidth = inBitmap.getWidth();
        int srcHeight = inBitmap.getHeight();
        if (sampling > srcWidth || sampling > srcHeight) {
            sampling = 1;
        }
        int scaledWidth = srcWidth / sampling;
        int scaledHeight = srcHeight / sampling;

        // Alpha is required for this transformation.
        Bitmap toTransform = getAlphaSafeBitmap(pool, inBitmap);
        Bitmap result = pool.get(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);

        result.setHasAlpha(true);

        BITMAP_DRAWABLE_LOCK.lock();
        try {
            Canvas canvas = new Canvas(result);
            canvas.scale(1 / (float) sampling, 1 / (float) sampling);
            Paint paint = new Paint();
            paint.setFlags(Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(toTransform, 0, 0, paint);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                result = BlurUtils.renderScriptBlur(context, result, radius);
            } else {
                result = BlurUtils.fastBlur(result, radius, true);
            }
            clear(canvas);
        } finally {
            BITMAP_DRAWABLE_LOCK.unlock();
        }

        if (!toTransform.equals(inBitmap)) {
            pool.put(toTransform);
        }

        return result;
    }

    private static Bitmap getAlphaSafeBitmap(@NonNull BitmapPool pool,
                                             @NonNull Bitmap maybeAlphaSafe) {
        if (Bitmap.Config.ARGB_8888.equals(maybeAlphaSafe.getConfig())) {
            return maybeAlphaSafe;
        }

        Bitmap argbBitmap = pool.get(maybeAlphaSafe.getWidth(), maybeAlphaSafe.getHeight(),
                Bitmap.Config.ARGB_8888);
        new Canvas(argbBitmap).drawBitmap(maybeAlphaSafe, 0 /*left*/, 0 /*top*/, null /*pain*/);

        // We now own this Bitmap. It's our responsibility to replace it in the pool outside this method
        // when we're finished with it.
        return argbBitmap;
    }

    // Avoids warnings in M+.
    private static void clear(Canvas canvas) {
        canvas.setBitmap(null);
    }

    private static Bitmap.Config getSafeConfig(Bitmap bitmap) {
        return bitmap.getConfig() != null ? bitmap.getConfig() : Bitmap.Config.ARGB_8888;
    }

    private static final class NoLock implements Lock {

        @Synthetic
        NoLock() {
        }

        @Override
        public void lock() {
            // do nothing
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            // do nothing
        }

        @Override
        public boolean tryLock() {
            return true;
        }

        @Override
        public boolean tryLock(long time, @NonNull TimeUnit unit) throws InterruptedException {
            return true;
        }

        @Override
        public void unlock() {
            // do nothing
        }

        @NonNull
        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException("Should not be called");
        }
    }
}

