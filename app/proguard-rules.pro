# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Room database classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# Keep app model classes
-keep class com.appdrawer.fast.models.** { *; }

# Keep search engine classes
-keep class com.appdrawer.fast.utils.SearchEngine { *; }

# Keep Kotlin coroutines
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# Keep app info for serialization
-keepclassmembers class com.appdrawer.fast.models.AppInfo {
    <fields>;
}

# Keep RecyclerView adapter
-keep class com.appdrawer.fast.adapters.** { *; }

# Android support libraries
-dontwarn android.support.**
-keep class android.support.** { *; }

# Material Design components
-keep class com.google.android.material.** { *; } 