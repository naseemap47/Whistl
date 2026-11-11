# Proguard rules for Whistl app

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# Google AdMob (API 35 optional classes when compileSdk is 34)
-dontwarn android.media.LoudnessCodecController**
-dontwarn android.media.LoudnessCodecController$*

