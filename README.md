# Simple Video Trimmerw
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![](https://jitpack.io/v/mohamed0017/SimpleVideoEditor.svg)](https://jitpack.io/#mohamed0017/SimpleVideoEditor)

<table>
    <tr><td align="center"><img src="https://github.com/mohamed0017/SimpleVideoEditor/blob/master/Screenshots/Screenshot_2023-08-26-16-58-44-339_com.video.sample%20(1).jpg" alt="Video Editor" width="100%"></td>
    <tr><td align="center"><b>Video Editor</b></td>
</table>

## About Library
Simple video editor Library contains the following features (cropping/trimming/compressing) videos, using FFmpegKit Libary. 

## Implementation
### [1] In your app module gradle file
```gradle
dependencies {
 implementation 'com.github.mohamed0017:SimpleVideoEditor:<latest_version>'
}
```

### [2] In your project level gradle file
```gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```
### [3] Use VideoTrimmer in your layout.xml
```xml
    <com.video.trimmer.view.VideoEditor
        android:id="@+id/videoTrimmer"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_below="@+id/header"/>
```
### [4] Implement OnVideoEditedListener on your Activity/ Fragment
```kotlin
class MainActivity : AppCompatActivity(), OnTrimVideoListener {
    ...
    override fun onTrimStarted(){
    }
    override fun getResult(uri: Uri){
    }
    override fun cancelAction(){
    }
    override fun onError(message: String){
    }
    override fun onProgress(percentage: Int){
    }
}

```
### [5] Create instances and set default values for the VideoTrimmer in your Activity/ Fragment
```kotlin
videoTrimmer.setTextTimeSelectionTypeface(FontsHelper[this, FontsConstants.SEMI_BOLD])
                    .setOnTrimVideoListener(this)
                    .setOnVideoListener(this)
                    .setVideoURI(Uri.parse(path))
                    .setVideoInformationVisibility(true)
                    .setMaxDuration(10)
                    .setMinDuration(2)
                    .setVideoQuality(VideoQuality.Medium) // set video quality
                    .setDestinationPath(Environment.getExternalStorageDirectory().path + File.separator + Environment.DIRECTORY_MOVIES)
```
### [8] Create instances and set default values for the VideoCropper in your Activity/ Fragment
```kotlin
 videoCropper.setVideoURI(Uri.parse(path))
                    .setOnCropVideoListener(this)
                    .setMinMaxRatios(0.3f, 3f)
                    .setDestinationPath(Environment.getExternalStorageDirectory().path + File.separator + Environment.DIRECTORY_MOVIES)
```

Voila! You have implemented an awesome Video Editor for your Android Project now!

Developed By 
------------

* Mohamed Hussien - <m.hussien.m44@gmail.com> Forked from <a href="https://github.com/tizisdeepan/VideoEditor"> Tizisdeepan </a>

<a href="https://www.linkedin.com/in/mohamed-hussien-a5608613b">
  <img alt="Add me to Linkedin" src="./Screenshots/linkedin.png" />
</a>
