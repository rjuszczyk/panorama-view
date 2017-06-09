# Panorama

PanoramaView for displaying 360 images.
Supports gyroscope rotation, it is calibrated with magnetometer to prevent gyroscope drift and to provide every time the same orientation.
If there is no gyroscope available it can be rotated by touch.

# Download

* Grab via Gradle:
```groovy
compile 'pl.rjuszczyk:panorama-view:0.0.3'
```
* or Maven:
```xml
<dependency>
  <groupId>pl.rjuszczyk</groupId>
  <artifactId>panorama-view</artifactId>
  <version>0.0.3</version>
  <type>pom</type>
</dependency>
```

# Features

* Same orientation (calibrated with magnetometer and accelerometer)
* No drift (smoofly recalibrated during displaying panorma)
* Pinch to zoom
* Rotating by touch
* Rotating by touch and by gyroscope in the same time - work in progress.
* Cubic panoramas - work in progress.

# Useage

* image from resources

```xml
<pl.rjuszczyk.panorama.viewer.PanoramaGLSurfaceView
    android:id="@+id/panorama"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:img="@drawable/pano2"
/>
```

```java
panoramaGLSurfaceView.setTexDrawableResourceID(R.drawable.pano2);
```

* image from glide

```java
Glide.with(getApplicationContext())
    .load("http://yourserver.com/panoramas/you-panorama-file.jpg")
    .asBitmap()
    .into(new SimpleTarget<Bitmap>() {
        @Override
        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
            panoramaGLSurfaceView.setTex_bitmap(resource);
        }
     });
```
