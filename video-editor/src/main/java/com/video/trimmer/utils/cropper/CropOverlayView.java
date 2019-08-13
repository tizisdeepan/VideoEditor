package com.video.trimmer.utils.cropper;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.Arrays;

public class CropOverlayView extends View {

    private ScaleGestureDetector mScaleDetector;
    private boolean mMultiTouchEnabled;
    private final CropWindowHandler mCropWindowHandler = new CropWindowHandler();
    private CropWindowChangeListener mCropWindowChangeListener;
    private final RectF mDrawRect = new RectF();
    private Paint mBorderPaint;
    private Paint mBorderCornerPaint;
    private Paint mGuidelinePaint;
    private Paint mBackgroundPaint;
    private Path mPath = new Path();
    private final float[] mBoundsPoints = new float[8];
    private final RectF mCalcBounds = new RectF();
    private int mViewWidth;
    private int mViewHeight;
    private float mBorderCornerOffset;
    private float mBorderCornerLength;
    private float mInitialCropWindowPaddingRatio;
    private float mTouchRadius;
    private float mSnapRadius;
    private CropWindowMoveHandler mMoveHandler;
    private boolean mFixAspectRatio;
    private int mAspectRatioX;
    private int mAspectRatioY;
    private float mTargetAspectRatio = ((float) mAspectRatioX) / mAspectRatioY;
    private CropImageView.Guidelines mGuidelines;
    private CropImageView.CropShape mCropShape;
    private final Rect mInitialCropWindowRect = new Rect();
    private boolean initializedCropWindow;
    private Integer mOriginalLayerType;

    public CropOverlayView(Context context) {
        this(context, null);
    }

    public CropOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setCropWindowChangeListener(CropWindowChangeListener listener) {
        mCropWindowChangeListener = listener;
    }

    public RectF getCropWindowRect() {
        return mCropWindowHandler.getRect();
    }

    public void setCropWindowRect(RectF rect) {
        mCropWindowHandler.setRect(rect);
    }

    public void fixCurrentCropWindowRect() {
        RectF rect = getCropWindowRect();
        fixCropWindowRectByRules(rect);
        mCropWindowHandler.setRect(rect);
    }

    public void setBounds(float[] boundsPoints, int viewWidth, int viewHeight) {
        if (boundsPoints == null || !Arrays.equals(mBoundsPoints, boundsPoints)) {
            if (boundsPoints == null) {
                Arrays.fill(mBoundsPoints, 0);
            } else {
                System.arraycopy(boundsPoints, 0, mBoundsPoints, 0, boundsPoints.length);
            }
            mViewWidth = viewWidth;
            mViewHeight = viewHeight;
            RectF cropRect = mCropWindowHandler.getRect();
            if (cropRect.width() == 0 || cropRect.height() == 0) {
                initCropWindow();
            }
        }
    }

    public void resetCropOverlayView() {
        if (initializedCropWindow) {
            setCropWindowRect(BitmapUtils.EMPTY_RECT_F);
            initCropWindow();
            invalidate();
        }
    }

    public CropImageView.CropShape getCropShape() {
        return mCropShape;
    }

    public void setCropShape(CropImageView.CropShape cropShape) {
        if (mCropShape != cropShape) {
            mCropShape = cropShape;
            if (Build.VERSION.SDK_INT <= 17) {
                if (mCropShape == CropImageView.CropShape.OVAL) {
                    mOriginalLayerType = getLayerType();
                    if (mOriginalLayerType != View.LAYER_TYPE_SOFTWARE) {
                        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    } else {
                        mOriginalLayerType = null;
                    }
                } else if (mOriginalLayerType != null) {
                    setLayerType(mOriginalLayerType, null);
                    mOriginalLayerType = null;
                }
            }
            invalidate();
        }
    }

    public CropImageView.Guidelines getGuidelines() {
        return mGuidelines;
    }

    public void setGuidelines(CropImageView.Guidelines guidelines) {
        if (mGuidelines != guidelines) {
            mGuidelines = guidelines;
            if (initializedCropWindow) {
                invalidate();
            }
        }
    }

    public boolean isFixAspectRatio() {
        return mFixAspectRatio;
    }

    public void setFixedAspectRatio(boolean fixAspectRatio) {
        if (mFixAspectRatio != fixAspectRatio) {
            mFixAspectRatio = fixAspectRatio;
            if (initializedCropWindow) {
                initCropWindow();
                invalidate();
            }
        }
    }

    public int getAspectRatioX() {
        return mAspectRatioX;
    }

    public void setAspectRatioX(int aspectRatioX) {
        if (aspectRatioX <= 0) {
            throw new IllegalArgumentException("Cannot set aspect ratio value to a number less than or equal to 0."); //No I18N
        } else if (mAspectRatioX != aspectRatioX) {
            mAspectRatioX = aspectRatioX;
            mTargetAspectRatio = ((float) mAspectRatioX) / mAspectRatioY;
            if (initializedCropWindow) {
                initCropWindow();
                invalidate();
            }
        }
    }

    public int getAspectRatioY() {
        return mAspectRatioY;
    }

    public void setAspectRatioY(int aspectRatioY) {
        if (aspectRatioY <= 0) {
            throw new IllegalArgumentException("Cannot set aspect ratio value to a number less than or equal to 0."); //No I18N
        } else if (mAspectRatioY != aspectRatioY) {
            mAspectRatioY = aspectRatioY;
            mTargetAspectRatio = ((float) mAspectRatioX) / mAspectRatioY;
            if (initializedCropWindow) {
                initCropWindow();
                invalidate();
            }
        }
    }

    public void setSnapRadius(float snapRadius) {
        mSnapRadius = snapRadius;
    }

    public boolean setMultiTouchEnabled(boolean multiTouchEnabled) {
        if (mMultiTouchEnabled != multiTouchEnabled) {
            mMultiTouchEnabled = multiTouchEnabled;
            if (mMultiTouchEnabled && mScaleDetector == null) {
                mScaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
            }
            return true;
        }
        return false;
    }

    public void setMinCropResultSize(int minCropResultWidth, int minCropResultHeight) {
        mCropWindowHandler.setMinCropResultSize(minCropResultWidth, minCropResultHeight);
    }

    public void setMaxCropResultSize(int maxCropResultWidth, int maxCropResultHeight) {
        mCropWindowHandler.setMaxCropResultSize(maxCropResultWidth, maxCropResultHeight);
    }

    public void setCropWindowLimits(float maxWidth, float maxHeight, float scaleFactorWidth, float scaleFactorHeight) {
        mCropWindowHandler.setCropWindowLimits(maxWidth, maxHeight, scaleFactorWidth, scaleFactorHeight);
    }

    public Rect getInitialCropWindowRect() {
        return mInitialCropWindowRect;
    }

    public void setInitialCropWindowRect(Rect rect) {
        mInitialCropWindowRect.set(rect != null ? rect : BitmapUtils.EMPTY_RECT);
        if (initializedCropWindow) {
            initCropWindow();
            invalidate();
            callOnCropWindowChanged(false);
        }
    }

    public void resetCropWindowRect() {
        if (initializedCropWindow) {
            initCropWindow();
            invalidate();
            callOnCropWindowChanged(false);
        }
    }

    public void setInitialAttributeValues(CropImageOptions options) {
        mCropWindowHandler.setInitialAttributeValues(options);
        setCropShape(options.cropShape);
        setSnapRadius(options.snapRadius);
        setGuidelines(options.guidelines);
        setFixedAspectRatio(options.fixAspectRatio);
        setAspectRatioX(options.aspectRatioX);
        setAspectRatioY(options.aspectRatioY);
        setMultiTouchEnabled(options.multiTouchEnabled);
        mTouchRadius = options.touchRadius;
        mInitialCropWindowPaddingRatio = options.initialCropWindowPaddingRatio;
        mBorderPaint = getNewPaintOrNull(options.borderLineThickness, options.borderLineColor);
        mBorderCornerOffset = options.borderCornerOffset;
        mBorderCornerLength = options.borderCornerLength;
        mBorderCornerPaint = getNewPaintOrNull(options.borderCornerThickness, options.borderCornerColor);
        mGuidelinePaint = getNewPaintOrNull(options.guidelinesThickness, options.guidelinesColor);
        mBackgroundPaint = getNewPaint(options.backgroundColor);
    }

    private void initCropWindow() {
        float leftLimit = Math.max(BitmapUtils.getRectLeft(mBoundsPoints), 0);
        float topLimit = Math.max(BitmapUtils.getRectTop(mBoundsPoints), 0);
        float rightLimit = Math.min(BitmapUtils.getRectRight(mBoundsPoints), getWidth());
        float bottomLimit = Math.min(BitmapUtils.getRectBottom(mBoundsPoints), getHeight());
        if (rightLimit <= leftLimit || bottomLimit <= topLimit) {
            return;
        }
        RectF rect = new RectF();
        initializedCropWindow = true;
        float horizontalPadding = mInitialCropWindowPaddingRatio * (rightLimit - leftLimit);
        float verticalPadding = mInitialCropWindowPaddingRatio * (bottomLimit - topLimit);
        if (mInitialCropWindowRect.width() > 0 && mInitialCropWindowRect.height() > 0) {
            rect.left = leftLimit + mInitialCropWindowRect.left / mCropWindowHandler.getScaleFactorWidth();
            rect.top = topLimit + mInitialCropWindowRect.top / mCropWindowHandler.getScaleFactorHeight();
            rect.right = rect.left + mInitialCropWindowRect.width() / mCropWindowHandler.getScaleFactorWidth();
            rect.bottom = rect.top + mInitialCropWindowRect.height() / mCropWindowHandler.getScaleFactorHeight();
            rect.left = Math.max(leftLimit, rect.left);
            rect.top = Math.max(topLimit, rect.top);
            rect.right = Math.min(rightLimit, rect.right);
            rect.bottom = Math.min(bottomLimit, rect.bottom);
        } else if (mFixAspectRatio && rightLimit > leftLimit && bottomLimit > topLimit) {
            float bitmapAspectRatio = (rightLimit - leftLimit) / (bottomLimit - topLimit);
            if (bitmapAspectRatio > mTargetAspectRatio) {
                rect.top = topLimit + verticalPadding;
                rect.bottom = bottomLimit - verticalPadding;
                float centerX = getWidth() / 2f;
                mTargetAspectRatio = (float) mAspectRatioX / mAspectRatioY;
                float cropWidth = Math.max(mCropWindowHandler.getMinCropWidth(), rect.height() * mTargetAspectRatio);
                float halfCropWidth = cropWidth / 2f;
                rect.left = centerX - halfCropWidth;
                rect.right = centerX + halfCropWidth;
            } else {
                rect.left = leftLimit + horizontalPadding;
                rect.right = rightLimit - horizontalPadding;
                float centerY = getHeight() / 2f;
                float cropHeight = Math.max(mCropWindowHandler.getMinCropHeight(), rect.width() / mTargetAspectRatio);
                float halfCropHeight = cropHeight / 2f;
                rect.top = centerY - halfCropHeight;
                rect.bottom = centerY + halfCropHeight;
            }
        } else {
            rect.left = leftLimit + horizontalPadding;
            rect.top = topLimit + verticalPadding;
            rect.right = rightLimit - horizontalPadding;
            rect.bottom = bottomLimit - verticalPadding;
        }
        fixCropWindowRectByRules(rect);
        mCropWindowHandler.setRect(rect);
    }

    private void fixCropWindowRectByRules(RectF rect) {
        if (rect.width() < mCropWindowHandler.getMinCropWidth()) {
            float adj = (mCropWindowHandler.getMinCropWidth() - rect.width()) / 2;
            rect.left -= adj;
            rect.right += adj;
        }
        if (rect.height() < mCropWindowHandler.getMinCropHeight()) {
            float adj = (mCropWindowHandler.getMinCropHeight() - rect.height()) / 2;
            rect.top -= adj;
            rect.bottom += adj;
        }
        if (rect.width() > mCropWindowHandler.getMaxCropWidth()) {
            float adj = (rect.width() - mCropWindowHandler.getMaxCropWidth()) / 2;
            rect.left += adj;
            rect.right -= adj;
        }
        if (rect.height() > mCropWindowHandler.getMaxCropHeight()) {
            float adj = (rect.height() - mCropWindowHandler.getMaxCropHeight()) / 2;
            rect.top += adj;
            rect.bottom -= adj;
        }
        calculateBounds(rect);
        if (mCalcBounds.width() > 0 && mCalcBounds.height() > 0) {
            float leftLimit = Math.max(mCalcBounds.left, 0);
            float topLimit = Math.max(mCalcBounds.top, 0);
            float rightLimit = Math.min(mCalcBounds.right, getWidth());
            float bottomLimit = Math.min(mCalcBounds.bottom, getHeight());
            if (rect.left < leftLimit) {
                rect.left = leftLimit;
            }
            if (rect.top < topLimit) {
                rect.top = topLimit;
            }
            if (rect.right > rightLimit) {
                rect.right = rightLimit;
            }
            if (rect.bottom > bottomLimit) {
                rect.bottom = bottomLimit;
            }
        }
        if (mFixAspectRatio && Math.abs(rect.width() - rect.height() * mTargetAspectRatio) > 0.1) {
            if (rect.width() > rect.height() * mTargetAspectRatio) {
                float adj = Math.abs(rect.height() * mTargetAspectRatio - rect.width()) / 2;
                rect.left += adj;
                rect.right -= adj;
            } else {
                float adj = Math.abs(rect.width() / mTargetAspectRatio - rect.height()) / 2;
                rect.top += adj;
                rect.bottom -= adj;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);
        if (mCropWindowHandler.showGuidelines()) {
            if (mGuidelines == CropImageView.Guidelines.ON) {
                drawGuidelines(canvas);
            } else if (mGuidelines == CropImageView.Guidelines.ON_TOUCH && mMoveHandler != null) {
                drawGuidelines(canvas);
            }
        }
        drawBorders(canvas);
        drawCorners(canvas);
    }

    private void drawBackground(Canvas canvas) {
        RectF rect = mCropWindowHandler.getRect();
        float left = Math.max(BitmapUtils.getRectLeft(mBoundsPoints), 0);
        float top = Math.max(BitmapUtils.getRectTop(mBoundsPoints), 0);
        float right = Math.min(BitmapUtils.getRectRight(mBoundsPoints), getWidth());
        float bottom = Math.min(BitmapUtils.getRectBottom(mBoundsPoints), getHeight());
        if (mCropShape == CropImageView.CropShape.RECTANGLE) {
            if (!isNonStraightAngleRotated()) {
                canvas.drawRect(left, top, right, rect.top, mBackgroundPaint);
                canvas.drawRect(left, rect.bottom, right, bottom, mBackgroundPaint);
                canvas.drawRect(left, rect.top, rect.left, rect.bottom, mBackgroundPaint);
                canvas.drawRect(rect.right, rect.top, right, rect.bottom, mBackgroundPaint);
            } else {
                mPath.reset();
                mPath.moveTo(mBoundsPoints[0], mBoundsPoints[1]);
                mPath.lineTo(mBoundsPoints[2], mBoundsPoints[3]);
                mPath.lineTo(mBoundsPoints[4], mBoundsPoints[5]);
                mPath.lineTo(mBoundsPoints[6], mBoundsPoints[7]);
                mPath.close();
                canvas.save();
                canvas.clipPath(mPath, Region.Op.INTERSECT);
                canvas.clipRect(rect, Region.Op.XOR);
                canvas.drawRect(left, top, right, bottom, mBackgroundPaint);
                canvas.restore();
            }
        } else {
            mPath.reset();
            mDrawRect.set(rect.left, rect.top, rect.right, rect.bottom);
            mPath.addOval(mDrawRect, Path.Direction.CW);
            canvas.save();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutPath(mPath);
            } else {
                canvas.clipPath(mPath, Region.Op.XOR);
            }
            canvas.drawRect(left, top, right, bottom, mBackgroundPaint);
            canvas.restore();
        }
    }

    private void drawGuidelines(Canvas canvas) {
        if (mGuidelinePaint != null) {
            float sw = mBorderPaint != null ? mBorderPaint.getStrokeWidth() : 0;
            RectF rect = mCropWindowHandler.getRect();
            rect.inset(sw, sw);
            float oneThirdCropWidth = rect.width() / 3;
            float oneThirdCropHeight = rect.height() / 3;
            if (mCropShape == CropImageView.CropShape.OVAL) {
                float w = rect.width() / 2 - sw;
                float h = rect.height() / 2 - sw;
                float x1 = rect.left + oneThirdCropWidth;
                float x2 = rect.right - oneThirdCropWidth;
                float yv = (float) (h * Math.sin(Math.acos((w - oneThirdCropWidth) / w)));
                canvas.drawLine(x1, rect.top + h - yv, x1, rect.bottom - h + yv, mGuidelinePaint);
                canvas.drawLine(x2, rect.top + h - yv, x2, rect.bottom - h + yv, mGuidelinePaint);
                float y1 = rect.top + oneThirdCropHeight;
                float y2 = rect.bottom - oneThirdCropHeight;
                float xv = (float) (w * Math.cos(Math.asin((h - oneThirdCropHeight) / h)));
                canvas.drawLine(rect.left + w - xv, y1, rect.right - w + xv, y1, mGuidelinePaint);
                canvas.drawLine(rect.left + w - xv, y2, rect.right - w + xv, y2, mGuidelinePaint);
            } else {
                float x1 = rect.left + oneThirdCropWidth;
                float x2 = rect.right - oneThirdCropWidth;
                canvas.drawLine(x1, rect.top, x1, rect.bottom, mGuidelinePaint);
                canvas.drawLine(x2, rect.top, x2, rect.bottom, mGuidelinePaint);
                float y1 = rect.top + oneThirdCropHeight;
                float y2 = rect.bottom - oneThirdCropHeight;
                canvas.drawLine(rect.left, y1, rect.right, y1, mGuidelinePaint);
                canvas.drawLine(rect.left, y2, rect.right, y2, mGuidelinePaint);
            }
        }
    }

    private void drawBorders(Canvas canvas) {
        if (mBorderPaint != null) {
            float w = mBorderPaint.getStrokeWidth();
            RectF rect = mCropWindowHandler.getRect();
            rect.inset(w / 2, w / 2);
            if (mCropShape == CropImageView.CropShape.RECTANGLE) {
                canvas.drawRect(rect, mBorderPaint);
            } else {
                canvas.drawOval(rect, mBorderPaint);
            }
        }
    }

    private void drawCorners(Canvas canvas) {
        if (mBorderCornerPaint != null) {
            float lineWidth = mBorderPaint != null ? mBorderPaint.getStrokeWidth() : 0;
            float cornerWidth = mBorderCornerPaint.getStrokeWidth();
            float w = cornerWidth / 2 + (mCropShape == CropImageView.CropShape.RECTANGLE ? mBorderCornerOffset : 0);
            RectF rect = mCropWindowHandler.getRect();
            rect.inset(w, w);
            float cornerOffset = (cornerWidth - lineWidth) / 2;
            float cornerExtension = cornerWidth / 2 + cornerOffset;
            canvas.drawLine(rect.left - cornerOffset, rect.top - cornerExtension, rect.left - cornerOffset, rect.top + mBorderCornerLength, mBorderCornerPaint);
            canvas.drawLine(rect.left - cornerExtension, rect.top - cornerOffset, rect.left + mBorderCornerLength, rect.top - cornerOffset, mBorderCornerPaint);
            canvas.drawLine(rect.right + cornerOffset, rect.top - cornerExtension, rect.right + cornerOffset, rect.top + mBorderCornerLength, mBorderCornerPaint);
            canvas.drawLine(rect.right + cornerExtension, rect.top - cornerOffset, rect.right - mBorderCornerLength, rect.top - cornerOffset, mBorderCornerPaint);
            canvas.drawLine(rect.left - cornerOffset, rect.bottom + cornerExtension, rect.left - cornerOffset, rect.bottom - mBorderCornerLength, mBorderCornerPaint);
            canvas.drawLine(rect.left - cornerExtension, rect.bottom + cornerOffset, rect.left + mBorderCornerLength, rect.bottom + cornerOffset, mBorderCornerPaint);
            canvas.drawLine(rect.right + cornerOffset, rect.bottom + cornerExtension, rect.right + cornerOffset, rect.bottom - mBorderCornerLength, mBorderCornerPaint);
            canvas.drawLine(rect.right + cornerExtension, rect.bottom + cornerOffset, rect.right - mBorderCornerLength, rect.bottom + cornerOffset, mBorderCornerPaint);
        }
    }

    private static Paint getNewPaint(int color) {
        Paint paint = new Paint();
        paint.setColor(color);
        return paint;
    }

    private static Paint getNewPaintOrNull(float thickness, int color) {
        if (thickness > 0) {
            Paint borderPaint = new Paint();
            borderPaint.setColor(color);
            borderPaint.setStrokeWidth(thickness);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setAntiAlias(true);
            return borderPaint;
        } else {
            return null;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isEnabled()) {
            if (mMultiTouchEnabled) {
                mScaleDetector.onTouchEvent(event);
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    onActionDown(event.getX(), event.getY());
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    getParent().requestDisallowInterceptTouchEvent(false);
                    onActionUp();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    onActionMove(event.getX(), event.getY());
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    private void onActionDown(float x, float y) {
        mMoveHandler = mCropWindowHandler.getMoveHandler(x, y, mTouchRadius, mCropShape);
        if (mMoveHandler != null) {
            invalidate();
        }
    }

    private void onActionUp() {
        if (mMoveHandler != null) {
            mMoveHandler = null;
            callOnCropWindowChanged(false);
            invalidate();
        }
    }

    private void onActionMove(float x, float y) {
        if (mMoveHandler != null) {
            float snapRadius = mSnapRadius;
            RectF rect = mCropWindowHandler.getRect();
            if (calculateBounds(rect)) {
                snapRadius = 0;
            }
            mMoveHandler.move(rect, x, y, mCalcBounds, mViewWidth, mViewHeight, snapRadius, mFixAspectRatio, mTargetAspectRatio);
            mCropWindowHandler.setRect(rect);
            callOnCropWindowChanged(true);
            invalidate();
        }
    }

    private boolean calculateBounds(RectF rect) {
        float left = BitmapUtils.getRectLeft(mBoundsPoints);
        float top = BitmapUtils.getRectTop(mBoundsPoints);
        float right = BitmapUtils.getRectRight(mBoundsPoints);
        float bottom = BitmapUtils.getRectBottom(mBoundsPoints);
        if (!isNonStraightAngleRotated()) {
            mCalcBounds.set(left, top, right, bottom);
            return false;
        } else {
            float x0 = mBoundsPoints[0];
            float y0 = mBoundsPoints[1];
            float x2 = mBoundsPoints[4];
            float y2 = mBoundsPoints[5];
            float x3 = mBoundsPoints[6];
            float y3 = mBoundsPoints[7];
            if (mBoundsPoints[7] < mBoundsPoints[1]) {
                if (mBoundsPoints[1] < mBoundsPoints[3]) {
                    x0 = mBoundsPoints[6];
                    y0 = mBoundsPoints[7];
                    x2 = mBoundsPoints[2];
                    y2 = mBoundsPoints[3];
                    x3 = mBoundsPoints[4];
                    y3 = mBoundsPoints[5];
                } else {
                    x0 = mBoundsPoints[4];
                    y0 = mBoundsPoints[5];
                    x2 = mBoundsPoints[0];
                    y2 = mBoundsPoints[1];
                    x3 = mBoundsPoints[2];
                    y3 = mBoundsPoints[3];
                }
            } else if (mBoundsPoints[1] > mBoundsPoints[3]) {
                x0 = mBoundsPoints[2];
                y0 = mBoundsPoints[3];
                x2 = mBoundsPoints[6];
                y2 = mBoundsPoints[7];
                x3 = mBoundsPoints[0];
                y3 = mBoundsPoints[1];
            }
            float a0 = (y3 - y0) / (x3 - x0);
            float a1 = -1f / a0;
            float b0 = y0 - a0 * x0;
            float b1 = y0 - a1 * x0;
            float b2 = y2 - a0 * x2;
            float b3 = y2 - a1 * x2;
            float c0 = (rect.centerY() - rect.top) / (rect.centerX() - rect.left);
            float c1 = -c0;
            float d0 = rect.top - c0 * rect.left;
            float d1 = rect.top - c1 * rect.right;
            left = Math.max(left, (d0 - b0) / (a0 - c0) < rect.right ? (d0 - b0) / (a0 - c0) : left);
            left = Math.max(left, (d0 - b1) / (a1 - c0) < rect.right ? (d0 - b1) / (a1 - c0) : left);
            left = Math.max(left, (d1 - b3) / (a1 - c1) < rect.right ? (d1 - b3) / (a1 - c1) : left);
            right = Math.min(right, (d1 - b1) / (a1 - c1) > rect.left ? (d1 - b1) / (a1 - c1) : right);
            right = Math.min(right, (d1 - b2) / (a0 - c1) > rect.left ? (d1 - b2) / (a0 - c1) : right);
            right = Math.min(right, (d0 - b2) / (a0 - c0) > rect.left ? (d0 - b2) / (a0 - c0) : right);
            top = Math.max(top, Math.max(a0 * left + b0, a1 * right + b1));
            bottom = Math.min(bottom, Math.min(a1 * left + b3, a0 * right + b2));
            mCalcBounds.left = left;
            mCalcBounds.top = top;
            mCalcBounds.right = right;
            mCalcBounds.bottom = bottom;
            return true;
        }
    }

    private boolean isNonStraightAngleRotated() {
        return mBoundsPoints[0] != mBoundsPoints[6] && mBoundsPoints[1] != mBoundsPoints[7];
    }

    private void callOnCropWindowChanged(boolean inProgress) {
        try {
            if (mCropWindowChangeListener != null) {
                mCropWindowChangeListener.onCropWindowChanged(inProgress);
            }
        } catch (Exception e) {
            Log.e("AIC", "Exception in crop window changed", e); //No I18N
        }
    }

    public interface CropWindowChangeListener {
        void onCropWindowChanged(boolean inProgress);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public boolean onScale(ScaleGestureDetector detector) {
            RectF rect = mCropWindowHandler.getRect();
            float x = detector.getFocusX();
            float y = detector.getFocusY();
            float dY = detector.getCurrentSpanY() / 2;
            float dX = detector.getCurrentSpanX() / 2;
            float newTop = y - dY;
            float newLeft = x - dX;
            float newRight = x + dX;
            float newBottom = y + dY;
            if (newLeft < newRight && newTop <= newBottom && newLeft >= 0 && newRight <= mCropWindowHandler.getMaxCropWidth() && newTop >= 0 && newBottom <= mCropWindowHandler.getMaxCropHeight()) {
                rect.set(newLeft, newTop, newRight, newBottom);
                mCropWindowHandler.setRect(rect);
                invalidate();
            }
            return true;
        }
    }
}