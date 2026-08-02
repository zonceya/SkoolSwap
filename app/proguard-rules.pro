# ============================================
# General Android / Kotlin
# ============================================
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes SourceFile, LineNumberTable
-keepattributes *Annotation*
-keepattributes Exceptions

# Keep native methods (JNI bridge)
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom Views (used from XML)
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Keep Parcelable implementations (needed by reflection)
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Enum values() / valueOf() (used by reflection)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep classes with @Keep annotation
-keep,allowobfuscation @androidx.annotation.Keep class *
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# ============================================
# Kotlin
# ============================================
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-dontwarn kotlin.**
-keep class kotlin.coroutines.Continuation

# ============================================
# Gson (if used) — reflection-based serialization
# ============================================
-keepattributes Signature
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep your model/data classes used with Gson
# Replace with your actual package
-keep class com.yourpackage.model.** { <fields>; }

# ============================================
# Retrofit / OkHttp (if used)
# ============================================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ============================================
# Room (if used)
# ============================================
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ============================================
# Firebase / Crashlytics (if used)
# ============================================
-keepattributes SourceFile, LineNumberTable
-keep public class * extends java.lang.Exception

# ============================================
# Aggressive shrinking / optimization
# ============================================
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''
# FirebaseUI-auth references old deprecated Smart Lock Credentials API
# These classes were removed from play-services-auth; safe to ignore
-dontwarn com.google.android.gms.auth.api.credentials.**

# Crashlytics buildtools reference compile-time-only annotation classes
-dontwarn com.google.firebase.crashlytics.buildtools.reloc.afu.org.checkerframework.**
-dontwarn com.google.firebase.crashlytics.buildtools.reloc.org.checkerframework.**
# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}