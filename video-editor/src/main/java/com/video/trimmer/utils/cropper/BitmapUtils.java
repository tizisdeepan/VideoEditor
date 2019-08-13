package com.video.trimmer.utils.cropper;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

final class BitmapUtils {

    static final Rect EMPTY_RECT = new Rect();
    static final RectF EMPTY_RECT_F = new RectF();
    static final RectF RECT = new RectF();
    static final float[] POINTS = new float[6];
    static final float[] POINTS2 = new float[6];
    private static int mMaxTextureSize;
    static Pair<String, WeakReference<Bitmap>> mStateBitmap;

    static BitmapSampled decodeSampledBitmap(Context context, Uri uri, int reqWidth, int reqHeight) {

        try {
            ContentResolver resolver = context.getContentResolver();
            BitmapFactory.Options options = decodeImageForOption(resolver, uri);
            options.inSampleSize = Math.max(calculateInSampleSizeByReqestedSize(options.outWidth, options.outHeight, reqWidth, reqHeight), calculateInSampleSizeByMaxTextureSize(options.outWidth, options.outHeight));
            Bitmap bitmap = decodeImage(resolver, uri, options);
            return new BitmapSampled(bitmap, options.inSampleSize);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load sampled bitmap: " + uri + "\r\n" + e.getMessage(), e); //No I18N
        }
    }

    static BitmapSampled cropBitmapObjectHandleOOM(
            Bitmap bitmap,
            float[] points,
            int degreesRotated,
            boolean fixAspectRatio,
            int aspectRatioX,
            int aspectRatioY,
            boolean flipHorizontally,
            boolean flipVertically) {
        int scale = 1;
        while (true) {
            try {
                Bitmap cropBitmap = cropBitmapObjectWithScale(bitmap, points, degreesRotated, fixAspectRatio, aspectRatioX, aspectRatioY, 1 / (float) scale, flipHorizontally, flipVertically);
                return new BitmapSampled(cropBitmap, scale);
            } catch (OutOfMemoryError e) {
                scale *= 2;
                if (scale > 8) {
                    throw e;
                }
            }
        }
    }

    private static Bitmap cropBitmapObjectWithScale(Bitmap bitmap, float[] points, int degreesRotated, boolean fixAspectRatio, int aspectRatioX, int aspectRatioY, float scale, boolean flipHorizontally, boolean flipVertically) {
        Rect rect = getRectFromPoints(points, bitmap.getWidth(), bitmap.getHeight(), fixAspectRatio, aspectRatioX, aspectRatioY);
        Matrix matrix = new Matrix();
        matrix.setRotate(degreesRotated, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
        matrix.postScale(flipHorizontally ? -scale : scale, flipVertically ? -scale : scale);
        Bitmap result = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height(), matrix, true);
        if (result == bitmap) {
            result = bitmap.copy(bitmap.getConfig(), false);
        }
        if (degreesRotated % 90 != 0) {
            result = cropForRotatedImage(result, points, rect, degreesRotated, fixAspectRatio, aspectRatioX, aspectRatioY);
        }
        return result;
    }

    static BitmapSampled cropBitmap(Context context, Uri loadedImageUri, float[] points, int degreesRotated, int orgWidth, int orgHeight, boolean fixAspectRatio, int aspectRatioX, int aspectRatioY, int reqWidth, int reqHeight, boolean flipHorizontally, boolean flipVertically) {
        int sampleMulti = 1;
        while (true) {
            try {
                return cropBitmap(context, loadedImageUri, points, degreesRotated, orgWidth, orgHeight, fixAspectRatio, aspectRatioX, aspectRatioY, reqWidth, reqHeight, flipHorizontally, flipVertically, sampleMulti);
            } catch (OutOfMemoryError e) {
                sampleMulti *= 2;
                if (sampleMulti > 16) {
                    throw new RuntimeException(
                            "Failed to handle OOM by sampling (" //No I18N
                                    + sampleMulti
                                    + "): "
                                    + loadedImageUri
                                    + "\r\n" //No I18N
                                    + e.getMessage(),
                            e);
                }
            }
        }
    }

    static float getRectLeft(float[] points) {
        return Math.min(Math.min(Math.min(points[0], points[2]), points[4]), points[6]);
    }

    static float getRectTop(float[] points) {
        return Math.min(Math.min(Math.min(points[1], points[3]), points[5]), points[7]);
    }

    static float getRectRight(float[] points) {
        return Math.max(Math.max(Math.max(points[0], points[2]), points[4]), points[6]);
    }

    static float getRectBottom(float[] points) {
        return Math.max(Math.max(Math.max(points[1], points[3]), points[5]), points[7]);
    }

    static float getRectWidth(float[] points) {
        return getRectRight(points) - getRectLeft(points);
    }

    static float getRectHeight(float[] points) {
        return getRectBottom(points) - getRectTop(points);
    }

    static float getRectCenterX(float[] points) {
        return (getRectRight(points) + getRectLeft(points)) / 2f;
    }

    static float getRectCenterY(float[] points) {
        return (getRectBottom(points) + getRectTop(points)) / 2f;
    }

    static Rect getRectFromPoints(float[] points, int imageWidth, int imageHeight, boolean fixAspectRatio, int aspectRatioX, int aspectRatioY) {
        int left = Math.round(Math.max(0, getRectLeft(points)));
        int top = Math.round(Math.max(0, getRectTop(points)));
        int right = Math.round(Math.min(imageWidth, getRectRight(points)));
        int bottom = Math.round(Math.min(imageHeight, getRectBottom(points)));
        Rect rect = new Rect(left, top, right, bottom);
        if (fixAspectRatio) {
            fixRectForAspectRatio(rect, aspectRatioX, aspectRatioY);
        }
        return rect;
    }

    private static void fixRectForAspectRatio(Rect rect, int aspectRatioX, int aspectRatioY) {
        if (aspectRatioX == aspectRatioY && rect.width() != rect.height()) {
            if (rect.height() > rect.width()) {
                rect.bottom -= rect.height() - rect.width();
            } else {
                rect.right -= rect.width() - rect.height();
            }
        }
    }

    static Uri writeTempStateStoreBitmap(Context context, Bitmap bitmap, Uri uri) {
        try {
            boolean needSave = true;
            if (uri == null) {
                uri = Uri.fromFile(File.createTempFile("aic_state_store_temp", ".jpg", context.getCacheDir())); //No I18N
            } else if (new File(uri.getPath()).exists()) {
                needSave = false;
            }
            if (needSave) {
                writeBitmapToUri(context, bitmap, uri, Bitmap.CompressFormat.JPEG, 95);
            }
            return uri;
        } catch (Exception e) {
            Log.w("AIC", "Failed to write bitmap to temp file for image-cropper save instance state", e); //No I18N
            return null;
        }
    }

    static void writeBitmapToUri(Context context, Bitmap bitmap, Uri uri, Bitmap.CompressFormat compressFormat, int compressQuality) throws FileNotFoundException {
        OutputStream outputStream = null;
        try {
            outputStream = context.getContentResolver().openOutputStream(uri);
            bitmap.compress(compressFormat, compressQuality, outputStream);
        } finally {
            closeSafe(outputStream);
        }
    }

    static Bitmap resizeBitmap(
            Bitmap bitmap, int reqWidth, int reqHeight, CropImageView.RequestSizeOptions options) {
        try {
            if (reqWidth > 0 && reqHeight > 0 && (options == CropImageView.RequestSizeOptions.RESIZE_FIT || options == CropImageView.RequestSizeOptions.RESIZE_INSIDE || options == CropImageView.RequestSizeOptions.RESIZE_EXACT)) {
                Bitmap resized = null;
                if (options == CropImageView.RequestSizeOptions.RESIZE_EXACT) {
                    resized = Bitmap.createScaledBitmap(bitmap, reqWidth, reqHeight, false);
                } else {
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    float scale = Math.max(width / (float) reqWidth, height / (float) reqHeight);
                    if (scale > 1 || options == CropImageView.RequestSizeOptions.RESIZE_FIT) {
                        resized = Bitmap.createScaledBitmap(bitmap, (int) (width / scale), (int) (height / scale), false);
                    }
                }
                if (resized != null) {
                    if (resized != bitmap) {
                        bitmap.recycle();
                    }
                    return resized;
                }
            }
        } catch (Exception e) {
            Log.w("AIC", "Failed to resize cropped image, return bitmap before resize", e); //No I18N
        }
        return bitmap;
    }

    private static BitmapSampled cropBitmap(Context context, Uri loadedImageUri, float[] points, int degreesRotated, int orgWidth, int orgHeight, boolean fixAspectRatio, int aspectRatioX, int aspectRatioY, int reqWidth, int reqHeight, boolean flipHorizontally, boolean flipVertically, int sampleMulti) {
        Rect rect = getRectFromPoints(points, orgWidth, orgHeight, fixAspectRatio, aspectRatioX, aspectRatioY);
        int width = reqWidth > 0 ? reqWidth : rect.width();
        int height = reqHeight > 0 ? reqHeight : rect.height();
        Bitmap result = null;
        int sampleSize = 1;
        try {
            BitmapSampled bitmapSampled = decodeSampledBitmapRegion(context, loadedImageUri, rect, width, height, sampleMulti);
            result = bitmapSampled.bitmap;
            sampleSize = bitmapSampled.sampleSize;
        } catch (Exception ignored) {
        }

        if (result != null) {
            try {
                result = rotateAndFlipBitmapInt(result, degreesRotated, flipHorizontally, flipVertically);
                if (degreesRotated % 90 != 0) {
                    result = cropForRotatedImage(result, points, rect, degreesRotated, fixAspectRatio, aspectRatioX, aspectRatioY);
                }
            } catch (OutOfMemoryError e) {
                if (result != null) {
                    result.recycle();
                }
                throw e;
            }
            return new BitmapSampled(result, sampleSize);
        } else {
            return cropBitmap(context, loadedImageUri, points, degreesRotated, fixAspectRatio, aspectRatioX, aspectRatioY, sampleMulti, rect, width, height, flipHorizontally, flipVertically);
        }
    }

    private static BitmapSampled cropBitmap(Context context, Uri loadedImageUri, float[] points, int degreesRotated, boolean fixAspectRatio, int aspectRatioX, int aspectRatioY, int sampleMulti, Rect rect, int width, int height, boolean flipHorizontally, boolean flipVertically) {
        Bitmap result = null;
        int sampleSize;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize = sampleMulti * calculateInSampleSizeByReqestedSize(rect.width(), rect.height(), width, height);
            Bitmap fullBitmap = decodeImage(context.getContentResolver(), loadedImageUri, options);
            if (fullBitmap != null) {
                try {
                    float[] points2 = new float[points.length];
                    System.arraycopy(points, 0, points2, 0, points.length);
                    for (int i = 0; i < points2.length; i++) {
                        points2[i] = points2[i] / options.inSampleSize;
                    }
                    result = cropBitmapObjectWithScale(fullBitmap, points2, degreesRotated, fixAspectRatio, aspectRatioX, aspectRatioY, 1, flipHorizontally, flipVertically);
                } finally {
                    if (result != fullBitmap) {
                        fullBitmap.recycle();
                    }
                }
            }
        } catch (OutOfMemoryError e) {
            if (result != null) {
                result.recycle();
            }
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to load sampled bitmap: " + loadedImageUri + "\r\n" + e.getMessage(), e); //No I18N
        }
        return new BitmapSampled(result, sampleSize);
    }

    private static BitmapFactory.Options decodeImageForOption(ContentResolver resolver, Uri uri) throws FileNotFoundException {
        InputStream stream = null;
        try {
            stream = resolver.openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(stream, EMPTY_RECT, options);
            options.inJustDecodeBounds = false;
            return options;
        } finally {
            closeSafe(stream);
        }
    }

    private static Bitmap decodeImage(ContentResolver resolver, Uri uri, BitmapFactory.Options options) throws FileNotFoundException {
        do {
            InputStream stream = null;
            try {
                stream = resolver.openInputStream(uri);
                return BitmapFactory.decodeStream(stream, EMPTY_RECT, options);
            } catch (OutOfMemoryError e) {
                options.inSampleSize *= 2;
            } finally {
                closeSafe(stream);
            }
        } while (options.inSampleSize <= 512);
        throw new RuntimeException("Failed to decode image: " + uri); //No I18N
    }

    private static BitmapSampled decodeSampledBitmapRegion(Context context, Uri uri, Rect rect, int reqWidth, int reqHeight, int sampleMulti) {
        InputStream stream = null;
        BitmapRegionDecoder decoder = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleMulti * calculateInSampleSizeByReqestedSize(rect.width(), rect.height(), reqWidth, reqHeight);
            stream = context.getContentResolver().openInputStream(uri);
            decoder = BitmapRegionDecoder.newInstance(stream, false);
            do {
                try {
                    return new BitmapSampled(decoder.decodeRegion(rect, options), options.inSampleSize);
                } catch (OutOfMemoryError e) {
                    options.inSampleSize *= 2;
                }
            } while (options.inSampleSize <= 512);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load sampled bitmap: " + uri + "\r\n" + e.getMessage(), e); //No I18N
        } finally {
            closeSafe(stream);
            if (decoder != null) {
                decoder.recycle();
            }
        }
        return new BitmapSampled(null, 1);
    }

    private static Bitmap cropForRotatedImage(Bitmap bitmap, float[] points, Rect rect, int degreesRotated, boolean fixAspectRatio, int aspectRatioX, int aspectRatioY) {
        if (degreesRotated % 90 != 0) {
            int adjLeft = 0, adjTop = 0, width = 0, height = 0;
            double rads = Math.toRadians(degreesRotated);
            int compareTo = degreesRotated < 90 || (degreesRotated > 180 && degreesRotated < 270) ? rect.left : rect.right;
            for (int i = 0; i < points.length; i += 2) {
                if (points[i] >= compareTo - 1 && points[i] <= compareTo + 1) {
                    adjLeft = (int) Math.abs(Math.sin(rads) * (rect.bottom - points[i + 1]));
                    adjTop = (int) Math.abs(Math.cos(rads) * (points[i + 1] - rect.top));
                    width = (int) Math.abs((points[i + 1] - rect.top) / Math.sin(rads));
                    height = (int) Math.abs((rect.bottom - points[i + 1]) / Math.cos(rads));
                    break;
                }
            }
            rect.set(adjLeft, adjTop, adjLeft + width, adjTop + height);
            if (fixAspectRatio) {
                fixRectForAspectRatio(rect, aspectRatioX, aspectRatioY);
            }
            Bitmap bitmapTmp = bitmap;
            bitmap = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height());
            if (bitmapTmp != bitmap) {
                bitmapTmp.recycle();
            }
        }
        return bitmap;
    }

    private static int calculateInSampleSizeByReqestedSize(int width, int height, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            while ((height / 2 / inSampleSize) > reqHeight && (width / 2 / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private static int calculateInSampleSizeByMaxTextureSize(int width, int height) {
        int inSampleSize = 1;
        if (mMaxTextureSize == 0) {
            mMaxTextureSize = getMaxTextureSize();
        }
        if (mMaxTextureSize > 0) {
            while ((height / inSampleSize) > mMaxTextureSize || (width / inSampleSize) > mMaxTextureSize) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private static Bitmap rotateAndFlipBitmapInt(Bitmap bitmap, int degrees, boolean flipHorizontally, boolean flipVertically) {
        if (degrees > 0 || flipHorizontally || flipVertically) {
            Matrix matrix = new Matrix();
            matrix.setRotate(degrees);
            matrix.postScale(flipHorizontally ? -1 : 1, flipVertically ? -1 : 1);
            Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            if (newBitmap != bitmap) {
                bitmap.recycle();
            }
            return newBitmap;
        } else {
            return bitmap;
        }
    }

    private static int getMaxTextureSize() {
        final int IMAGE_MAX_BITMAP_DIMENSION = 2048;
        try {
            EGL10 egl = (EGL10) EGLContext.getEGL();
            EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            int[] version = new int[2];
            egl.eglInitialize(display, version);
            int[] totalConfigurations = new int[1];
            egl.eglGetConfigs(display, null, 0, totalConfigurations);
            EGLConfig[] configurationsList = new EGLConfig[totalConfigurations[0]];
            egl.eglGetConfigs(display, configurationsList, totalConfigurations[0], totalConfigurations);
            int[] textureSize = new int[1];
            int maximumTextureSize = 0;
            for (int i = 0; i < totalConfigurations[0]; i++) {
                egl.eglGetConfigAttrib(display, configurationsList[i], EGL10.EGL_MAX_PBUFFER_WIDTH, textureSize);
                if (maximumTextureSize < textureSize[0]) {
                    maximumTextureSize = textureSize[0];
                }
            }
            egl.eglTerminate(display);
            return Math.max(maximumTextureSize, IMAGE_MAX_BITMAP_DIMENSION);
        } catch (Exception e) {
            return IMAGE_MAX_BITMAP_DIMENSION;
        }
    }

    private static void closeSafe(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }

    static final class BitmapSampled {

        public final Bitmap bitmap;
        final int sampleSize;

        BitmapSampled(Bitmap bitmap, int sampleSize) {
            this.bitmap = bitmap;
            this.sampleSize = sampleSize;
        }
    }

    static final class RotateBitmapResult {

        public final Bitmap bitmap;
        final int degrees;

        RotateBitmapResult(Bitmap bitmap, int degrees) {
            this.bitmap = bitmap;
            this.degrees = degrees;
        }
    }
}
