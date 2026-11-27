# LumaCraft Photo Studio - R8 rules
-keep class com.lumacraft.studio.** { *; }

# Google ML Kit on-device segmentation (JNI + native bindings)
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_** { *; }
-keep class com.google.android.odml.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.odml.**

# ExifInterface & Coil
-keep class androidx.exifinterface.** { *; }
-dontwarn coil.**
