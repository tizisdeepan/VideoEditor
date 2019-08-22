# Video Trimmer
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![](https://jitpack.io/v/tizisdeepan/videoeditor.svg)](https://jitpack.io/#tizisdeepan/videoeditor)

<table>
    <tr><td align="center"><img src="https://github.com/tizisdeepan/VideoEditor/blob/master/Screenshots/ve1.png" alt="Video Trimmer" width="100%"></td>
    <td align="center"><img src="https://github.com/tizisdeepan/VideoEditor/blob/master/Screenshots/ve2.png" alt="Video Cropper" width="100%"></td></tr>
    <tr><td align="center"><b>Video Trimmer</b></td>
    <td align="center"><b>Video Cropper</b></td></tr>
</table>

## About Library
FFmpeg is a powerful multimedia framework which allows us to decode, encode, transcode, stream, filter and play most of the media content available now. With the help of these tools, you can develop and application that can manipulate any form of media to the desired output. Sky is not the limit when using FFmpeg. I prefer FFmpeg-all which is a Bible for FFmpeg but it is difficult to read if you do not know what you are looking for. To make it easy, I will summarise the basics of video manipulations using FFmpeg which, then you can use in your own applications or libraries that you are working on. I have developed a simple library that enables you to trim and crop a video, additionally you can compress any video and convert it into any format that you desire.

## Implementation
### [1] In your app module gradle file
```gradle
dependencies {
    implementation 'com.github.tizisdeepan:videoeditor:<latest_version>'
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
<com.video.trimmer.view.VideoTrimmer
        android:id="@+id/videoTrimmer"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_below="@+id/header"/>
```
### [4] Use VideoCropper in your layout.xml
```xml
<com.video.trimmer.view.VideoCropper
        android:id="@+id/videoCropper"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_below="@+id/header"/>
```
### [5] Implement OnTrimVideoListener on your Activity/ Fragment
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
}
```
### [6] Implement OnCropVideoListener on your Activity/ Fragment
```kotlin
class MainActivity : AppCompatActivity(), OnTrimVideoListener {
    ...
    override fun onCropStarted(){
    }
    override fun getResult(uri: Uri){
    }
    override fun cancelAction(){
    }
    override fun onError(message: String){
    }
    override fun onProgress(progress: Float){
    }
}
```
### [7] Create instances and set default values for the VideoTrimmer in your Activity/ Fragment
```kotlin
videoTrimmer.setTextTimeSelectionTypeface(FontsHelper[this, FontsConstants.SEMI_BOLD])
                    .setOnTrimVideoListener(this)
                    .setOnVideoListener(this)
                    .setVideoURI(Uri.parse(path))
                    .setVideoInformationVisibility(true)
                    .setMaxDuration(10)
                    .setMinDuration(2)
                    .setDestinationPath(Environment.getExternalStorageDirectory().toString() + File.separator + "temp" + File.separator + "Videos" + File.separator)
```
### [8] Create instances and set default values for the VideoCropper in your Activity/ Fragment
```kotlin
 videoCropper.setVideoURI(Uri.parse(path))
                    .setOnCropVideoListener(this)
                    .setMinMaxRatios(0.3f, 3f)
                    .setDestinationPath(Environment.getExternalStorageDirectory().toString() + File.separator + "temp" + File.separator + "Videos" + File.separator)
```

Voila! You have implemented an awesome Video Editor for your Android Project now!

Developed By
------------

* Deepan Elango - <tizisdeepan@gmail.com>

<a href="https://twitter.com/tizisdeepan">
  <img alt="Follow me on Twitter" src="./Screenshots/twitter.png" />
</a>
<a href="https://www.linkedin.com/in/tizisdeepan/">
  <img alt="Add me to Linkedin" src="./Screenshots/linkedin.png" />
</a>
