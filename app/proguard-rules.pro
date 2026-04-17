# Tripsyc Android — ProGuard / R8 rules
# See https://developer.android.com/studio/build/shrink-code

# Preserve annotations, signatures, and exceptions (needed by Retrofit / reflection).
-keepattributes Signature, Exceptions, *Annotation*, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable

# ───── Kotlin ─────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Keep coroutine internals R8 sometimes over-strips.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ───── Retrofit / OkHttp ──────────────────────────────────────────────────────
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**

# ───── Gson ───────────────────────────────────────────────────────────────────
-keep class com.google.gson.** { *; }
-keepattributes EnclosingMethod
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep all Tripsyc API models (Gson reflects on fields).
-keep class com.tripsyc.app.data.api.models.** { *; }
-keepclassmembers class com.tripsyc.app.data.api.models.** {
    <init>(...);
    <fields>;
}

# ───── Firebase Messaging ─────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.tripsyc.app.push.** { *; }
-dontwarn com.google.firebase.**

# ───── Coil ───────────────────────────────────────────────────────────────────
-dontwarn coil.**

# ───── Compose ────────────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ───── App entry points ───────────────────────────────────────────────────────
-keep class com.tripsyc.app.MainActivity { *; }
-keep class com.tripsyc.app.TripsycApp { *; }

# Remove Log.d / Log.v in release.
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
