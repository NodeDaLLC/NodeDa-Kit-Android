# Rules applied to apps that depend on this library.
# kotlinx.serialization needs companion serializer accessors preserved.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class com.nodeda.sdk.** {
    *** Companion;
}
-keepclasseswithmembers class com.nodeda.sdk.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp internal classes that reflectively reference platform code on JDK 9+.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
