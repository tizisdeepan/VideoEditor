package com.video.trimmer.utils.cropper;

import android.graphics.RectF;

final class CropWindowHandler {

    private final RectF mEdges = new RectF();
    private final RectF mGetEdges = new RectF();
    private float mMinCropWindowWidth;
    private float mMinCropWindowHeight;
    private float mMaxCropWindowWidth;
    private float mMaxCropWindowHeight;
    private float mMinCropResultWidth;
    private float mMinCropResultHeight;
    private float mMaxCropResultWidth;
    private float mMaxCropResultHeight;
    private float mScaleFactorWidth = 1;
    private float mScaleFactorHeight = 1;

    public RectF getRect() {
        mGetEdges.set(mEdges);
        return mGetEdges;
    }

    public float getMinCropWidth() {
        return Math.max(mMinCropWindowWidth, mMinCropResultWidth / mScaleFactorWidth);
    }

    public float getMinCropHeight() {
        return Math.max(mMinCropWindowHeight, mMinCropResultHeight / mScaleFactorHeight);
    }

    public float getMaxCropWidth() {
        return Math.min(mMaxCropWindowWidth, mMaxCropResultWidth / mScaleFactorWidth);
    }

    public float getMaxCropHeight() {
        return Math.min(mMaxCropWindowHeight, mMaxCropResultHeight / mScaleFactorHeight);
    }

    public float getScaleFactorWidth() {
        return mScaleFactorWidth;
    }

    public float getScaleFactorHeight() {
        return mScaleFactorHeight;
    }

    public void setMinCropResultSize(int minCropResultWidth, int minCropResultHeight) {
        mMinCropResultWidth = minCropResultWidth;
        mMinCropResultHeight = minCropResultHeight;
    }

    public void setMaxCropResultSize(int maxCropResultWidth, int maxCropResultHeight) {
        mMaxCropResultWidth = maxCropResultWidth;
        mMaxCropResultHeight = maxCropResultHeight;
    }

    public void setCropWindowLimits(
            float maxWidth, float maxHeight, float scaleFactorWidth, float scaleFactorHeight) {
        mMaxCropWindowWidth = maxWidth;
        mMaxCropWindowHeight = maxHeight;
        mScaleFactorWidth = scaleFactorWidth;
        mScaleFactorHeight = scaleFactorHeight;
    }

    public void setInitialAttributeValues(CropImageOptions options) {
        mMinCropWindowWidth = options.minCropWindowWidth;
        mMinCropWindowHeight = options.minCropWindowHeight;
        mMinCropResultWidth = options.minCropResultWidth;
        mMinCropResultHeight = options.minCropResultHeight;
        mMaxCropResultWidth = options.maxCropResultWidth;
        mMaxCropResultHeight = options.maxCropResultHeight;
    }

    public void setRect(RectF rect) {
        mEdges.set(rect);
    }

    public boolean showGuidelines() {
        return !(mEdges.width() < 100 || mEdges.height() < 100);
    }

    public CropWindowMoveHandler getMoveHandler(float x, float y, float targetRadius, CropImageView.CropShape cropShape) {
        CropWindowMoveHandler.Type type = cropShape == CropImageView.CropShape.OVAL ? getOvalPressedMoveType(x, y) : getRectanglePressedMoveType(x, y, targetRadius);
        return type != null ? new CropWindowMoveHandler(type, this, x, y) : null;
    }

    private CropWindowMoveHandler.Type getRectanglePressedMoveType(float x, float y, float targetRadius) {
        CropWindowMoveHandler.Type moveType = null;
        if (CropWindowHandler.isInCornerTargetZone(x, y, mEdges.left, mEdges.top, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.TOP_LEFT;
        } else if (CropWindowHandler.isInCornerTargetZone(x, y, mEdges.right, mEdges.top, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.TOP_RIGHT;
        } else if (CropWindowHandler.isInCornerTargetZone(x, y, mEdges.left, mEdges.bottom, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.BOTTOM_LEFT;
        } else if (CropWindowHandler.isInCornerTargetZone(x, y, mEdges.right, mEdges.bottom, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.BOTTOM_RIGHT;
        } else if (CropWindowHandler.isInCenterTargetZone(x, y, mEdges.left, mEdges.top, mEdges.right, mEdges.bottom) && focusCenter()) {
            moveType = CropWindowMoveHandler.Type.CENTER;
        } else if (CropWindowHandler.isInHorizontalTargetZone(x, y, mEdges.left, mEdges.right, mEdges.top, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.TOP;
        } else if (CropWindowHandler.isInHorizontalTargetZone(x, y, mEdges.left, mEdges.right, mEdges.bottom, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.BOTTOM;
        } else if (CropWindowHandler.isInVerticalTargetZone(x, y, mEdges.left, mEdges.top, mEdges.bottom, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.LEFT;
        } else if (CropWindowHandler.isInVerticalTargetZone(x, y, mEdges.right, mEdges.top, mEdges.bottom, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.RIGHT;
        } else if (CropWindowHandler.isInCenterTargetZone(x, y, mEdges.left, mEdges.top, mEdges.right, mEdges.bottom) && !focusCenter()) {
            moveType = CropWindowMoveHandler.Type.CENTER;
        }
        return moveType;
    }

    private CropWindowMoveHandler.Type getOvalPressedMoveType(float x, float y) {

        float cellLength = mEdges.width() / 6;
        float leftCenter = mEdges.left + cellLength;
        float rightCenter = mEdges.left + (5 * cellLength);
        float cellHeight = mEdges.height() / 6;
        float topCenter = mEdges.top + cellHeight;
        float bottomCenter = mEdges.top + 5 * cellHeight;

        CropWindowMoveHandler.Type moveType;
        if (x < leftCenter) {
            if (y < topCenter) {
                moveType = CropWindowMoveHandler.Type.TOP_LEFT;
            } else if (y < bottomCenter) {
                moveType = CropWindowMoveHandler.Type.LEFT;
            } else {
                moveType = CropWindowMoveHandler.Type.BOTTOM_LEFT;
            }
        } else if (x < rightCenter) {
            if (y < topCenter) {
                moveType = CropWindowMoveHandler.Type.TOP;
            } else if (y < bottomCenter) {
                moveType = CropWindowMoveHandler.Type.CENTER;
            } else {
                moveType = CropWindowMoveHandler.Type.BOTTOM;
            }
        } else {
            if (y < topCenter) {
                moveType = CropWindowMoveHandler.Type.TOP_RIGHT;
            } else if (y < bottomCenter) {
                moveType = CropWindowMoveHandler.Type.RIGHT;
            } else {
                moveType = CropWindowMoveHandler.Type.BOTTOM_RIGHT;
            }
        }
        return moveType;
    }

    private static boolean isInCornerTargetZone(float x, float y, float handleX, float handleY, float targetRadius) {
        return Math.abs(x - handleX) <= targetRadius && Math.abs(y - handleY) <= targetRadius;
    }

    private static boolean isInHorizontalTargetZone(float x, float y, float handleXStart, float handleXEnd, float handleY, float targetRadius) {
        return x > handleXStart && x < handleXEnd && Math.abs(y - handleY) <= targetRadius;
    }

    private static boolean isInVerticalTargetZone(float x, float y, float handleX, float handleYStart, float handleYEnd, float targetRadius) {
        return Math.abs(x - handleX) <= targetRadius && y > handleYStart && y < handleYEnd;
    }

    private static boolean isInCenterTargetZone(float x, float y, float left, float top, float right, float bottom) {
        return x > left && x < right && y > top && y < bottom;
    }

    private boolean focusCenter() {
        return !showGuidelines();
    }
}
