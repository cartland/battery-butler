# ProGuard rules for Battery Butler

# Keep generic signatures for Kotlin (required for type-safe serialization)
-keepattributes Signature
-keepattributes *Annotation*

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Kotlin Serialization
-keepattributes InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# kotlin-inject generated code
-keep class **Component { *; }
-keep class **Component$* { *; }
-keepclassmembers class * {
    @me.tatarka.inject.annotations.* <methods>;
    @me.tatarka.inject.annotations.* <fields>;
}

# Room database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Wire protobuf
-keep class com.squareup.wire.** { *; }
-keep class com.chriscartland.batterybutler.proto.** { *; }

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Compose - keep stability metadata
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Domain models (keep for reflection if needed)
-keep class com.chriscartland.batterybutler.domain.model.** { *; }

# Preserve line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
