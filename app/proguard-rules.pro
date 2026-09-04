# Anonymous Gson TypeToken subclasses resolve their type argument through
# getGenericSuperclass(), which needs Signature *and* the InnerClasses /
# EnclosingMethod attributes. Without them R8 release builds die on startup with
# "Missing type parameter." from BaseConverter.genericType() while ObjectBox is
# registering its custom property converters.
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-dontobfuscate
# NB: R8 ignores -optimizations entirely; kept only for ProGuard compatibility.
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

# Gson reflection (these ship as consumer rules in Gson 2.10+, which we predate).
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
-dontwarn sun.misc.**

-keepclassmembers class com.fastaccess.** { *; }
-keep class com.fastaccess.** { *; }
-keepclassmembers class com.prettifier.** { *; }
-keep class com.prettifier.** { *; }
-keepclassmembers class com.zzhoujay.** { *; }
-keep class com.zzhoujay.** { *; }

-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE