package com.video.trimmer.utils.cropper;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class CropImageOptions implements Parcelable {

  public static final Creator<CropImageOptions> CREATOR =
      new Creator<CropImageOptions>() {
        @Override
        public CropImageOptions createFromParcel(Parcel in) {
          return new CropImageOptions(in);
        }

        @Override
        public CropImageOptions[] newArray(int size) {
          return new CropImageOptions[size];
        }
      };


  public CropImageView.CropShape cropShape;
  public float snapRadius;
  public float touchRadius;
  public CropImageView.Guidelines guidelines;
  public CropImageView.ScaleType scaleType;
  public boolean showCropOverlay;
  public boolean showProgressBar;
  public boolean autoZoomEnabled;
  public boolean multiTouchEnabled;
  public int maxZoom;
  public float initialCropWindowPaddingRatio;
  public boolean fixAspectRatio;
  public int aspectRatioX;
  public int aspectRatioY;
  public float borderLineThickness;
  public int borderLineColor;
  public float borderCornerThickness;
  public float borderCornerOffset;
  public float borderCornerLength;
  public int borderCornerColor;
  public float guidelinesThickness;
  public int guidelinesColor;
  public int backgroundColor;
  public int minCropWindowWidth;
  public int minCropWindowHeight;
  public int minCropResultWidth;
  public int minCropResultHeight;
  public int maxCropResultWidth;
  public int maxCropResultHeight;
  public CharSequence activityTitle;
  public int activityMenuIconColor;
  public Uri outputUri;
  public Bitmap.CompressFormat outputCompressFormat;
  public int outputCompressQuality;
  public int outputRequestWidth;
  public int outputRequestHeight;
  public CropImageView.RequestSizeOptions outputRequestSizeOptions;
  public boolean noOutputImage;
  public Rect initialCropWindowRectangle;
  public int initialRotation;
  public boolean allowRotation;
  public boolean allowFlipping;
  public boolean allowCounterRotation;
  public int rotationDegrees;
  public boolean flipHorizontally;
  public boolean flipVertically;
  public CharSequence cropMenuCropButtonTitle;
  public int cropMenuCropButtonIcon;

  CropImageOptions() {
    DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();

    cropShape = CropImageView.CropShape.RECTANGLE;
    snapRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, dm);
    touchRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, dm);
    guidelines = CropImageView.Guidelines.ON_TOUCH;
    scaleType = CropImageView.ScaleType.FIT_CENTER;
    showCropOverlay = true;
    showProgressBar = true;
    autoZoomEnabled = true;
    multiTouchEnabled = false;
    maxZoom = 4;
    initialCropWindowPaddingRatio = 0.1f;

    fixAspectRatio = false;
    aspectRatioX = 1;
    aspectRatioY = 1;

    borderLineThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, dm);
    borderLineColor = Color.argb(170, 255, 255, 255);
    borderCornerThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, dm);
    borderCornerOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, dm);
    borderCornerLength = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, dm);
    borderCornerColor = Color.WHITE;

    guidelinesThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, dm);
    guidelinesColor = Color.argb(170, 255, 255, 255);
    backgroundColor = Color.argb(119, 0, 0, 0);

    minCropWindowWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42, dm);
    minCropWindowHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42, dm);
    minCropResultWidth = 40;
    minCropResultHeight = 40;
    maxCropResultWidth = 99999;
    maxCropResultHeight = 99999;

    activityTitle = "";
    activityMenuIconColor = 0;

    outputUri = Uri.EMPTY;
    outputCompressFormat = Bitmap.CompressFormat.JPEG;
    outputCompressQuality = 90;
    outputRequestWidth = 0;
    outputRequestHeight = 0;
    outputRequestSizeOptions = CropImageView.RequestSizeOptions.NONE;
    noOutputImage = false;

    initialCropWindowRectangle = null;
    initialRotation = -1;
    allowRotation = true;
    allowFlipping = true;
    allowCounterRotation = false;
    rotationDegrees = 90;
    flipHorizontally = false;
    flipVertically = false;
    cropMenuCropButtonTitle = null;

    cropMenuCropButtonIcon = 0;
  }

  private CropImageOptions(Parcel in) {
    cropShape = CropImageView.CropShape.values()[in.readInt()];
    snapRadius = in.readFloat();
    touchRadius = in.readFloat();
    guidelines = CropImageView.Guidelines.values()[in.readInt()];
    scaleType = CropImageView.ScaleType.values()[in.readInt()];
    showCropOverlay = in.readByte() != 0;
    showProgressBar = in.readByte() != 0;
    autoZoomEnabled = in.readByte() != 0;
    multiTouchEnabled = in.readByte() != 0;
    maxZoom = in.readInt();
    initialCropWindowPaddingRatio = in.readFloat();
    fixAspectRatio = in.readByte() != 0;
    aspectRatioX = in.readInt();
    aspectRatioY = in.readInt();
    borderLineThickness = in.readFloat();
    borderLineColor = in.readInt();
    borderCornerThickness = in.readFloat();
    borderCornerOffset = in.readFloat();
    borderCornerLength = in.readFloat();
    borderCornerColor = in.readInt();
    guidelinesThickness = in.readFloat();
    guidelinesColor = in.readInt();
    backgroundColor = in.readInt();
    minCropWindowWidth = in.readInt();
    minCropWindowHeight = in.readInt();
    minCropResultWidth = in.readInt();
    minCropResultHeight = in.readInt();
    maxCropResultWidth = in.readInt();
    maxCropResultHeight = in.readInt();
    activityTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
    activityMenuIconColor = in.readInt();
    outputUri = in.readParcelable(Uri.class.getClassLoader());
    outputCompressFormat = Bitmap.CompressFormat.valueOf(in.readString());
    outputCompressQuality = in.readInt();
    outputRequestWidth = in.readInt();
    outputRequestHeight = in.readInt();
    outputRequestSizeOptions = CropImageView.RequestSizeOptions.values()[in.readInt()];
    noOutputImage = in.readByte() != 0;
    initialCropWindowRectangle = in.readParcelable(Rect.class.getClassLoader());
    initialRotation = in.readInt();
    allowRotation = in.readByte() != 0;
    allowFlipping = in.readByte() != 0;
    allowCounterRotation = in.readByte() != 0;
    rotationDegrees = in.readInt();
    flipHorizontally = in.readByte() != 0;
    flipVertically = in.readByte() != 0;
    cropMenuCropButtonTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
    cropMenuCropButtonIcon = in.readInt();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(cropShape.ordinal());
    dest.writeFloat(snapRadius);
    dest.writeFloat(touchRadius);
    dest.writeInt(guidelines.ordinal());
    dest.writeInt(scaleType.ordinal());
    dest.writeByte((byte) (showCropOverlay ? 1 : 0));
    dest.writeByte((byte) (showProgressBar ? 1 : 0));
    dest.writeByte((byte) (autoZoomEnabled ? 1 : 0));
    dest.writeByte((byte) (multiTouchEnabled ? 1 : 0));
    dest.writeInt(maxZoom);
    dest.writeFloat(initialCropWindowPaddingRatio);
    dest.writeByte((byte) (fixAspectRatio ? 1 : 0));
    dest.writeInt(aspectRatioX);
    dest.writeInt(aspectRatioY);
    dest.writeFloat(borderLineThickness);
    dest.writeInt(borderLineColor);
    dest.writeFloat(borderCornerThickness);
    dest.writeFloat(borderCornerOffset);
    dest.writeFloat(borderCornerLength);
    dest.writeInt(borderCornerColor);
    dest.writeFloat(guidelinesThickness);
    dest.writeInt(guidelinesColor);
    dest.writeInt(backgroundColor);
    dest.writeInt(minCropWindowWidth);
    dest.writeInt(minCropWindowHeight);
    dest.writeInt(minCropResultWidth);
    dest.writeInt(minCropResultHeight);
    dest.writeInt(maxCropResultWidth);
    dest.writeInt(maxCropResultHeight);
    TextUtils.writeToParcel(activityTitle, dest, flags);
    dest.writeInt(activityMenuIconColor);
    dest.writeParcelable(outputUri, flags);
    dest.writeString(outputCompressFormat.name());
    dest.writeInt(outputCompressQuality);
    dest.writeInt(outputRequestWidth);
    dest.writeInt(outputRequestHeight);
    dest.writeInt(outputRequestSizeOptions.ordinal());
    dest.writeInt(noOutputImage ? 1 : 0);
    dest.writeParcelable(initialCropWindowRectangle, flags);
    dest.writeInt(initialRotation);
    dest.writeByte((byte) (allowRotation ? 1 : 0));
    dest.writeByte((byte) (allowFlipping ? 1 : 0));
    dest.writeByte((byte) (allowCounterRotation ? 1 : 0));
    dest.writeInt(rotationDegrees);
    dest.writeByte((byte) (flipHorizontally ? 1 : 0));
    dest.writeByte((byte) (flipVertically ? 1 : 0));
    TextUtils.writeToParcel(cropMenuCropButtonTitle, dest, flags);
    dest.writeInt(cropMenuCropButtonIcon);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public void validate() {
    if (maxZoom < 0) {
      throw new IllegalArgumentException("Cannot set max zoom to a number < 1"); //No I18N
    }
    if (touchRadius < 0) {
      throw new IllegalArgumentException("Cannot set touch radius value to a number <= 0 "); //No I18N
    }
    if (initialCropWindowPaddingRatio < 0 || initialCropWindowPaddingRatio >= 0.5) {
      throw new IllegalArgumentException("Cannot set initial crop window padding value to a number < 0 or >= 0.5"); //No I18N
    }
    if (aspectRatioX <= 0) {
      throw new IllegalArgumentException("Cannot set aspect ratio value to a number less than or equal to 0."); //No I18N
    }
    if (aspectRatioY <= 0) {
      throw new IllegalArgumentException("Cannot set aspect ratio value to a number less than or equal to 0."); //No I18N
    }
    if (borderLineThickness < 0) {
      throw new IllegalArgumentException("Cannot set line thickness value to a number less than 0."); //No I18N
    }
    if (borderCornerThickness < 0) {
      throw new IllegalArgumentException("Cannot set corner thickness value to a number less than 0."); //No I18N
    }
    if (guidelinesThickness < 0) {
      throw new IllegalArgumentException("Cannot set guidelines thickness value to a number less than 0."); //No I18N
    }
    if (minCropWindowHeight < 0) {
      throw new IllegalArgumentException("Cannot set min crop window height value to a number < 0 "); //No I18N
    }
    if (minCropResultWidth < 0) {
      throw new IllegalArgumentException("Cannot set min crop result width value to a number < 0 "); //No I18N
    }
    if (minCropResultHeight < 0) {
      throw new IllegalArgumentException("Cannot set min crop result height value to a number < 0 "); //No I18N
    }
    if (maxCropResultWidth < minCropResultWidth) {
      throw new IllegalArgumentException("Cannot set max crop result width to smaller value than min crop result width"); //No I18N
    }
    if (maxCropResultHeight < minCropResultHeight) {
      throw new IllegalArgumentException("Cannot set max crop result height to smaller value than min crop result height"); //No I18N
    }
    if (outputRequestWidth < 0) {
      throw new IllegalArgumentException("Cannot set request width value to a number < 0 "); //No I18N
    }
    if (outputRequestHeight < 0) {
      throw new IllegalArgumentException("Cannot set request height value to a number < 0 "); //No I18N
    }
    if (rotationDegrees < 0 || rotationDegrees > 360) {
      throw new IllegalArgumentException("Cannot set rotation degrees value to a number < 0 or > 360"); //No I18N
    }
  }
}
