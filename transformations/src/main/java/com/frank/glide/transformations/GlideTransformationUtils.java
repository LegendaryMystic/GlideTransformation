package com.frank.glide.transformations;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.ExifInterface;
import android.os.Build;
import android.renderscript.RSRuntimeException;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Synthetic;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A class with methods to efficiently resize Bitmaps.
 *
 * @author frank
 */
public final class GlideTransformationUtils {
    private static final String TAG = "TransformationUtils";
    public static final int PAINT_FLAGS = Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG;
    private static final Paint DEFAULT_PAINT = new Paint(PAINT_FLAGS);
    private static final int CIRCLE_CROP_PAINT_FLAGS = PAINT_FLAGS | Paint.ANTI_ALIAS_FLAG;
    private static final Paint CIRCLE_CROP_SHAPE_PAINT = new Paint(CIRCLE_CROP_PAINT_FLAGS);
    private static final Paint CIRCLE_CROP_BITMAP_PAINT;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CORNER_TYPE_ALL, CORNER_TYPE_TOP_LEFT, CORNER_TYPE_TOP_RIGHT, CORNER_TYPE_BOTTOM_LEFT,
            CORNER_TYPE_BOTTOM_RIGHT, CORNER_TYPE_TOP, CORNER_TYPE_BOTTOM, CORNER_TYPE_LEFT,
            CORNER_TYPE_RIGHT, CORNER_TYPE_OTHER_TOP_LEFT, CORNER_TYPE_OTHER_TOP_RIGHT,
            CORNER_TYPE_OTHER_BOTTOM_LEFT, CORNER_TYPE_OTHER_BOTTOM_RIGHT,
            CORNER_TYPE_DIAGONAL_FROM_TOP_LEFT, CORNER_TYPE_DIAGONAL_FROM_TOP_RIGHT})
    @interface CornerType {
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CROP_TYPE_TOP, CROP_TYPE_CENTER, CROP_TYPE_BOTTOM})
    @interface CropType {
    }

    public static final int CORNER_TYPE_ALL = 0;
    public static final int CORNER_TYPE_TOP_LEFT = 1;
    public static final int CORNER_TYPE_TOP_RIGHT = 2;
    public static final int CORNER_TYPE_BOTTOM_LEFT = 3;
    public static final int CORNER_TYPE_BOTTOM_RIGHT = 4;
    public static final int CORNER_TYPE_TOP = 5;
    public static final int CORNER_TYPE_BOTTOM = 6;
    public static final int CORNER_TYPE_LEFT = 7;
    public static final int CORNER_TYPE_RIGHT = 8;
    public static final int CORNER_TYPE_OTHER_TOP_LEFT = 9;
    public static final int CORNER_TYPE_OTHER_TOP_RIGHT = 10;
    public static final int CORNER_TYPE_OTHER_BOTTOM_LEFT = 11;
    public static final int CORNER_TYPE_OTHER_BOTTOM_RIGHT = 12;
    public static final int CORNER_TYPE_DIAGONAL_FROM_TOP_LEFT = 13;
    public static final int CORNER_TYPE_DIAGONAL_FROM_TOP_RIGHT = 14;

    public static final int CROP_TYPE_TOP = 0;
    public static final int CROP_TYPE_CENTER = 1;
    public static final int CROP_TYPE_BOTTOM = 2;

    // See #738.
    private static final List<String> MODELS_REQUIRING_BITMAP_LOCK =
            Arrays.asList(
                    // Moto X gen 2
                    "XT1085",
                    "XT1092",
                    "XT1093",
                    "XT1094",
                    "XT1095",
                    "XT1096",
                    "XT1097",
                    "XT1098"
            );
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

    static {
        CIRCLE_CROP_BITMAP_PAINT = new Paint(CIRCLE_CROP_PAINT_FLAGS);
        CIRCLE_CROP_BITMAP_PAINT.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    }

    private GlideTransformationUtils() {
        // Utility class.
    }

    public static Lock getBitmapDrawableLock() {
        return BITMAP_DRAWABLE_LOCK;
    }

    /**
     * Sets the alpha of the Bitmap we're going to re-use to the alpha of the Bitmap we're going to
     * transform. This keeps {@link android.graphics.Bitmap#hasAlpha()}} consistent before and after
     * the transformation for transformations that don't add or remove transparent pixels.
     *
     * @param inBitmap  The {@link android.graphics.Bitmap} that will be transformed.
     * @param outBitmap The {@link android.graphics.Bitmap} that will be returned from the
     *                  transformation.
     */
    public static void setAlpha(Bitmap inBitmap, Bitmap outBitmap) {
        outBitmap.setHasAlpha(inBitmap.hasAlpha());
    }

    /**
     * This is an expensive operation that copies the image in place with the pixels rotated. If
     * possible rather use getOrientationMatrix, and put that as the imageMatrix on an ImageView.
     *
     * @param imageToOrient   Image Bitmap to orient.
     * @param degreesToRotate number of degrees to rotate the image by. If zero the original image is
     *                        returned unmodified.
     * @return The oriented bitmap. May be the imageToOrient without modification, or a new Bitmap.
     */
    public static Bitmap rotateImage(@NonNull Bitmap imageToOrient, int degreesToRotate) {
        Bitmap result = imageToOrient;
        try {
            if (degreesToRotate != 0) {
                Matrix matrix = new Matrix();
                matrix.setRotate(degreesToRotate);
                result = Bitmap.createBitmap(imageToOrient, 0, 0, imageToOrient.getWidth(),
                        imageToOrient.getHeight(), matrix, true /*filter*/);
            }
        } catch (Exception e) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "Exception when trying to orient image", e);
            }
        }
        return result;
    }

    /**
     * Get the # of degrees an image must be rotated to match the given exif orientation.
     *
     * @param exifOrientation The exif orientation [1-8]
     * @return the number of degrees to rotate
     */
    public static int getExifOrientationDegrees(int exifOrientation) {
        final int degreesToRotate;
        switch (exifOrientation) {
            case ExifInterface.ORIENTATION_TRANSPOSE:
            case ExifInterface.ORIENTATION_ROTATE_90:
                degreesToRotate = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                degreesToRotate = 180;
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
            case ExifInterface.ORIENTATION_ROTATE_270:
                degreesToRotate = 270;
                break;
            default:
                degreesToRotate = 0;
                break;
        }
        return degreesToRotate;
    }

    /**
     * Rotate and/or flip the image to match the given exif orientation.
     *
     * @param pool            A pool that may or may not contain an image of the necessary
     *                        dimensions.
     * @param inBitmap        The bitmap to rotate/flip.
     * @param exifOrientation the exif orientation [1-8].
     * @return The rotated and/or flipped image or toOrient if no rotation or flip was necessary.
     */
    public static Bitmap rotateImageExif(@NonNull BitmapPool pool, @NonNull Bitmap inBitmap,
                                         int exifOrientation) {
        if (!isExifOrientationRequired(exifOrientation)) {
            return inBitmap;
        }

        final Matrix matrix = new Matrix();
        initializeMatrixForRotation(exifOrientation, matrix);

        // From Bitmap.createBitmap.
        final RectF newRect = new RectF(0, 0, inBitmap.getWidth(), inBitmap.getHeight());
        matrix.mapRect(newRect);

        final int newWidth = Math.round(newRect.width());
        final int newHeight = Math.round(newRect.height());

        Bitmap.Config config = getSafeConfig(inBitmap);
        Bitmap result = pool.get(newWidth, newHeight, config);

        matrix.postTranslate(-newRect.left, -newRect.top);

        applyMatrix(inBitmap, result, matrix);
        return result;
    }

    /**
     * Returns {@code true} if the given exif orientation indicates that a transformation is necessary
     * and {@code false} otherwise.
     */
    public static boolean isExifOrientationRequired(int exifOrientation) {
        switch (exifOrientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
            case ExifInterface.ORIENTATION_ROTATE_180:
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
            case ExifInterface.ORIENTATION_TRANSPOSE:
            case ExifInterface.ORIENTATION_ROTATE_90:
            case ExifInterface.ORIENTATION_TRANSVERSE:
            case ExifInterface.ORIENTATION_ROTATE_270:
                return true;
            default:
                return false;
        }
    }

    private static Bitmap getAlphaSafeBitmap(@NonNull BitmapPool pool,
                                             @NonNull Bitmap maybeAlphaSafe) {
        if (Bitmap.Config.ARGB_8888.equals(maybeAlphaSafe.getConfig())) {
            return maybeAlphaSafe;
        }

        Bitmap argbBitmap = pool.get(maybeAlphaSafe.getWidth(), maybeAlphaSafe.getHeight(),
                Bitmap.Config.ARGB_8888);
        new Canvas(argbBitmap).drawBitmap(maybeAlphaSafe, 0 /*left*/, 0 /*top*/, null /*paint*/);

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

    private static void applyMatrix(@NonNull Bitmap inBitmap, @NonNull Bitmap targetBitmap,
                                    Matrix matrix) {
        BITMAP_DRAWABLE_LOCK.lock();
        try {
            Canvas canvas = new Canvas(targetBitmap);
            canvas.drawBitmap(inBitmap, matrix, DEFAULT_PAINT);
            clear(canvas);
        } finally {
            BITMAP_DRAWABLE_LOCK.unlock();
        }
    }

    // Visible for testing.
    static void initializeMatrixForRotation(int exifOrientation, Matrix matrix) {
        switch (exifOrientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                // Do nothing.
        }
    }

    public static Bitmap blur(@NonNull BitmapPool pool, @NonNull Bitmap inBitmap,
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
                try {
                    result = BlurUtils.renderScriptBlur(context, result, radius);
                } catch (RSRuntimeException e) {
                    result = BlurUtils.fastBlur(result, radius, true);
                }
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

    public static Bitmap roundedCorners(@NonNull BitmapPool pool, @NonNull Bitmap inBitmap,
                                        int radius, int margin,
                                        @CornerType int cornerType) {
        // Alpha is required for this transformation.
        Bitmap toTransform = getAlphaSafeBitmap(pool, inBitmap);
        Bitmap result =
                pool.get(toTransform.getWidth(), toTransform.getHeight(), Bitmap.Config.ARGB_8888);

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
            float right = result.getWidth() - margin;
            float bottom = result.getHeight() - margin;
            int diameter = radius * 2;

            switch (cornerType) {
                case CORNER_TYPE_ALL:
                    canvas.drawRoundRect(new RectF(margin, margin, right, bottom), radius, radius, paint);
                    break;
                case CORNER_TYPE_TOP_LEFT:
                    canvas.drawRoundRect(new RectF(margin, margin, margin + diameter, margin + diameter),
                            radius, radius, paint);
                    canvas.drawRect(new RectF(margin, margin + radius, margin + radius, bottom), paint);
                    canvas.drawRect(new RectF(margin + radius, margin, right, bottom), paint);
                    break;
                case CORNER_TYPE_TOP_RIGHT:
                    canvas.drawRoundRect(new RectF(right - diameter, margin, right, margin + diameter), radius,
                            radius, paint);
                    canvas.drawRect(new RectF(margin, margin, right - radius, bottom), paint);
                    canvas.drawRect(new RectF(right - radius, margin + radius, right, bottom), paint);
                    break;
                case CORNER_TYPE_BOTTOM_LEFT:
                    canvas.drawRoundRect(new RectF(margin, bottom - diameter, margin + diameter, bottom),
                            radius, radius, paint);
                    canvas.drawRect(new RectF(margin, margin, margin + diameter, bottom - radius), paint);
                    canvas.drawRect(new RectF(margin + radius, margin, right, bottom), paint);
                    break;
                case CORNER_TYPE_BOTTOM_RIGHT:
                    canvas.drawRoundRect(new RectF(right - diameter, bottom - diameter, right, bottom), radius,
                            radius, paint);
                    canvas.drawRect(new RectF(margin, margin, right - radius, bottom), paint);
                    canvas.drawRect(new RectF(right - radius, margin, right, bottom - radius), paint);
                    break;
                case CORNER_TYPE_TOP:
                    canvas.drawRoundRect(new RectF(margin, margin, right, margin + diameter), radius, radius,
                            paint);
                    canvas.drawRect(new RectF(margin, margin + radius, right, bottom), paint);
                    break;
                case CORNER_TYPE_BOTTOM:
                    canvas.drawRoundRect(new RectF(margin, bottom - diameter, right, bottom), radius, radius,
                            paint);
                    canvas.drawRect(new RectF(margin, margin, right, bottom - radius), paint);
                    break;
                case CORNER_TYPE_LEFT:
                    canvas.drawRoundRect(new RectF(margin, margin, margin + diameter, bottom), radius, radius,
                            paint);
                    canvas.drawRect(new RectF(margin + radius, margin, right, bottom), paint);
                    break;
                case CORNER_TYPE_RIGHT:
                    canvas.drawRoundRect(new RectF(right - diameter, margin, right, bottom), radius, radius,
                            paint);
                    canvas.drawRect(new RectF(margin, margin, right - radius, bottom), paint);
                    break;
                case CORNER_TYPE_OTHER_TOP_LEFT:
                    canvas.drawRoundRect(new RectF(margin, bottom - diameter, right, bottom), radius, radius,
                            paint);
                    canvas.drawRoundRect(new RectF(right - diameter, margin, right, bottom), radius, radius,
                            paint);
                    canvas.drawRect(new RectF(margin, margin, right - radius, bottom - radius), paint);
                    break;
                case CORNER_TYPE_OTHER_TOP_RIGHT:
                    canvas.drawRoundRect(new RectF(margin, margin, margin + diameter, bottom), radius, radius,
                            paint);
                    canvas.drawRoundRect(new RectF(margin, bottom - diameter, right, bottom), radius, radius,
                            paint);
                    canvas.drawRect(new RectF(margin + radius, margin, right, bottom - radius), paint);
                    break;
                case CORNER_TYPE_OTHER_BOTTOM_LEFT:
                    canvas.drawRoundRect(new RectF(margin, margin, right, margin + diameter), radius, radius,
                            paint);
                    canvas.drawRoundRect(new RectF(right - diameter, margin, right, bottom), radius, radius,
                            paint);
                    canvas.drawRect(new RectF(margin, margin + radius, right - radius, bottom), paint);
                    break;
                case CORNER_TYPE_OTHER_BOTTOM_RIGHT:
                    canvas.drawRoundRect(new RectF(margin, margin, right, margin + diameter), radius, radius,
                            paint);
                    canvas.drawRoundRect(new RectF(margin, margin, margin + diameter, bottom), radius, radius,
                            paint);
                    canvas.drawRect(new RectF(margin + radius, margin + radius, right, bottom), paint);
                    break;
                case CORNER_TYPE_DIAGONAL_FROM_TOP_LEFT:
                    canvas.drawRoundRect(new RectF(margin, margin, margin + diameter, margin + diameter),
                            radius, radius, paint);
                    canvas.drawRoundRect(new RectF(right - diameter, bottom - diameter, right, bottom), radius,
                            radius, paint);
                    canvas.drawRect(new RectF(margin, margin + radius, right - diameter, bottom), paint);
                    canvas.drawRect(new RectF(margin + diameter, margin, right, bottom - radius), paint);
                    break;
                case CORNER_TYPE_DIAGONAL_FROM_TOP_RIGHT:
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

    public static Bitmap crop(@NonNull BitmapPool pool, @NonNull Bitmap inBitmap, int width, int height,
                              @CornerType int cropType) {
        int srcWidth = inBitmap.getWidth();
        int srcHeight = inBitmap.getHeight();
        if (width == 0 && height != 0) {
            width = (srcWidth * height) / srcHeight;
        } else if (height == 0 && width != 0) {
            height = (srcHeight * width) / srcWidth;
        }
        width = width == 0 ? srcWidth : width;
        height = height == 0 ? srcHeight : height;
        // Alpha is required for this transformation.
        Bitmap toTransform = getAlphaSafeBitmap(pool, inBitmap);
        Bitmap result = pool.get(width, height, Bitmap.Config.ARGB_8888);

        result.setHasAlpha(true);

        BITMAP_DRAWABLE_LOCK.lock();
        try {
            float scaleX = (float) width / srcWidth;
            float scaleY = (float) height / srcHeight;
            float scale = Math.max(scaleX, scaleY);

            float scaledWidth = scale * srcWidth;
            float scaledHeight = scale * srcHeight;
            float left = (width - scaledWidth) / 2;
            float top;
            if (cropType == CROP_TYPE_TOP) {
                top = 0;
            } else if (cropType == CROP_TYPE_BOTTOM) {
                top = height - scaledHeight;
            } else {
                top = (height - scaledHeight) / 2;
            }
            RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

            Canvas canvas = new Canvas(result);
            canvas.drawBitmap(toTransform, null, targetRect, null);
            clear(canvas);
        } finally {
            BITMAP_DRAWABLE_LOCK.unlock();
        }

        if (!toTransform.equals(inBitmap)) {
            pool.put(toTransform);
        }

        return result;
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
