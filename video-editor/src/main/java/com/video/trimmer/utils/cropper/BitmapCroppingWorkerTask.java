package com.video.trimmer.utils.cropper;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

final class BitmapCroppingWorkerTask
        extends AsyncTask<Void, Void, BitmapCroppingWorkerTask.Result> {

    private final WeakReference<CropImageView> mCropImageViewReference;
    private final Bitmap mBitmap;
    private final Uri mUri;
    private final Context mContext;
    private final float[] mCropPoints;
    private final int mDegreesRotated;
    private final int mOrgWidth;
    private final int mOrgHeight;
    private final boolean mFixAspectRatio;
    private final int mAspectRatioX;
    private final int mAspectRatioY;
    private final int mReqWidth;
    private final int mReqHeight;
    private final boolean mFlipHorizontally;
    private final boolean mFlipVertically;
    private final CropImageView.RequestSizeOptions mReqSizeOptions;
    private final Uri mSaveUri;
    private final Bitmap.CompressFormat mSaveCompressFormat;
    private final int mSaveCompressQuality;

    BitmapCroppingWorkerTask(
            CropImageView cropImageView,
            Bitmap bitmap,
            float[] cropPoints,
            int degreesRotated,
            boolean fixAspectRatio,
            int aspectRatioX,
            int aspectRatioY,
            int reqWidth,
            int reqHeight,
            boolean flipHorizontally,
            boolean flipVertically,
            CropImageView.RequestSizeOptions options,
            Uri saveUri,
            Bitmap.CompressFormat saveCompressFormat,
            int saveCompressQuality) {

        mCropImageViewReference = new WeakReference<>(cropImageView);
        mContext = cropImageView.getContext();
        mBitmap = bitmap;
        mCropPoints = cropPoints;
        mUri = null;
        mDegreesRotated = degreesRotated;
        mFixAspectRatio = fixAspectRatio;
        mAspectRatioX = aspectRatioX;
        mAspectRatioY = aspectRatioY;
        mReqWidth = reqWidth;
        mReqHeight = reqHeight;
        mFlipHorizontally = flipHorizontally;
        mFlipVertically = flipVertically;
        mReqSizeOptions = options;
        mSaveUri = saveUri;
        mSaveCompressFormat = saveCompressFormat;
        mSaveCompressQuality = saveCompressQuality;
        mOrgWidth = 0;
        mOrgHeight = 0;
    }

    BitmapCroppingWorkerTask(
            CropImageView cropImageView,
            Uri uri,
            float[] cropPoints,
            int degreesRotated,
            int orgWidth,
            int orgHeight,
            boolean fixAspectRatio,
            int aspectRatioX,
            int aspectRatioY,
            int reqWidth,
            int reqHeight,
            boolean flipHorizontally,
            boolean flipVertically,
            CropImageView.RequestSizeOptions options,
            Uri saveUri,
            Bitmap.CompressFormat saveCompressFormat,
            int saveCompressQuality) {

        mCropImageViewReference = new WeakReference<>(cropImageView);
        mContext = cropImageView.getContext();
        mUri = uri;
        mCropPoints = cropPoints;
        mDegreesRotated = degreesRotated;
        mFixAspectRatio = fixAspectRatio;
        mAspectRatioX = aspectRatioX;
        mAspectRatioY = aspectRatioY;
        mOrgWidth = orgWidth;
        mOrgHeight = orgHeight;
        mReqWidth = reqWidth;
        mReqHeight = reqHeight;
        mFlipHorizontally = flipHorizontally;
        mFlipVertically = flipVertically;
        mReqSizeOptions = options;
        mSaveUri = saveUri;
        mSaveCompressFormat = saveCompressFormat;
        mSaveCompressQuality = saveCompressQuality;
        mBitmap = null;
    }

    public Uri getUri() {
        return mUri;
    }

    @Override
    protected BitmapCroppingWorkerTask.Result doInBackground(Void... params) {
        try {
            if (!isCancelled()) {

                BitmapUtils.BitmapSampled bitmapSampled;
                if (mUri != null) {
                    bitmapSampled =
                            BitmapUtils.cropBitmap(
                                    mContext,
                                    mUri,
                                    mCropPoints,
                                    mDegreesRotated,
                                    mOrgWidth,
                                    mOrgHeight,
                                    mFixAspectRatio,
                                    mAspectRatioX,
                                    mAspectRatioY,
                                    mReqWidth,
                                    mReqHeight,
                                    mFlipHorizontally,
                                    mFlipVertically);
                } else if (mBitmap != null) {
                    bitmapSampled =
                            BitmapUtils.cropBitmapObjectHandleOOM(
                                    mBitmap,
                                    mCropPoints,
                                    mDegreesRotated,
                                    mFixAspectRatio,
                                    mAspectRatioX,
                                    mAspectRatioY,
                                    mFlipHorizontally,
                                    mFlipVertically);
                } else {
                    return new Result((Bitmap) null, 1);
                }

                Bitmap bitmap =
                        BitmapUtils.resizeBitmap(bitmapSampled.bitmap, mReqWidth, mReqHeight, mReqSizeOptions);

                if (mSaveUri == null) {
                    return new Result(bitmap, bitmapSampled.sampleSize);
                } else {
                    BitmapUtils.writeBitmapToUri(
                            mContext, bitmap, mSaveUri, mSaveCompressFormat, mSaveCompressQuality);
                    if (bitmap != null) {
                        bitmap.recycle();
                    }
                    return new Result(mSaveUri, bitmapSampled.sampleSize);
                }
            }
            return null;
        } catch (Exception e) {
            return new Result(e, mSaveUri != null);
        }
    }

    @Override
    protected void onPostExecute(Result result) {
        if (result != null) {
            boolean completeCalled = false;
            if (!isCancelled()) {
                CropImageView cropImageView = mCropImageViewReference.get();
                if (cropImageView != null) {
                    completeCalled = true;
                    cropImageView.onImageCroppingAsyncComplete(result);
                }
            }
            if (!completeCalled && result.bitmap != null) {
                result.bitmap.recycle();
            }
        }
    }

    static final class Result {
        public final Bitmap bitmap;
        public final Uri uri;
        final Exception error;
        final boolean isSave;
        final int sampleSize;

        Result(Bitmap bitmap, int sampleSize) {
            this.bitmap = bitmap;
            this.uri = null;
            this.error = null;
            this.isSave = false;
            this.sampleSize = sampleSize;
        }

        Result(Uri uri, int sampleSize) {
            this.bitmap = null;
            this.uri = uri;
            this.error = null;
            this.isSave = true;
            this.sampleSize = sampleSize;
        }

        Result(Exception error, boolean isSave) {
            this.bitmap = null;
            this.uri = null;
            this.error = error;
            this.isSave = isSave;
            this.sampleSize = 1;
        }
    }
}
