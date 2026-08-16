# CPZ hardened baseline R8 rules.
# Keep only runtime-reflection/JNI surfaces that are still deliberately present.

-keepattributes *Annotation*
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# OkHttp / Retrofit reflection.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Media3.
-keep class androidx.media3.** { *; }

# Coroutines.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# QuickJS remains temporarily for source compatibility. Plugin installation/execution is
# fail-closed and the native loader is not called at application startup, but JNI symbols must not
# be renamed if a remaining code path is accidentally reached during this transitional build.
-keep class com.whl.quickjs.wrapper.** { *; }
-dontwarn com.whl.quickjs.wrapper.**

# IMPORTANT: no keep rules for com.lumora.scraper.providers/extractors. Their runtime registry is
# empty in the hardened branch; R8 is expected to strip unreachable implementations and their
# embedded third-party domains from the APK. Rhino, Java-WebSocket, NanoHTTPD and libtorrent4j
# dependencies were removed entirely and therefore have no keep/dontwarn rules here.
