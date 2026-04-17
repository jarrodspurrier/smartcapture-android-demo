######################################################
# Mobile Journey Capture Service SDK
#
# Minification rules for apps using MJCS
#
# Clients using MJCS will most likely need to copy these in
######################################################

######################
# Sample App Related #
######################

#keep R and ids
-keep class **.R
-keep class **.R$* {
    <fields>;
}
-keepparameternames
-renamesourcefileattribute SourceFile
-keepattributes Signature,Exceptions,*Annotation*,
                InnerClasses,PermittedSubclasses,EnclosingMethod,
                Deprecated,SourceFile,LineNumberTable

-repackageclasses 'com.idscan.mjcs.sample'
-allowaccessmodification
-keeppackagenames doNotKeepAThing

#######################
# Overall SDK Related #
#######################

-keep class **.BuildConfig { *; }

######################
# Doc Camera Related #
######################

-keepclasseswithmembernames,includedescriptorclasses class * { native <methods>; }

###########################
# Active Liveness Related #
###########################

-keep public class com.idscan.idfb.liveness.models.*
-keep class com.idscan.idfb.liveness.models.*
-keep enum com.idscan.idfb.liveness.models.*
-keep public enum com.idscan.idfb.liveness.models.*
-keep public class com.idscan.idfb.liveness.Liveness { *; }
-keep class android.support.v8.renderscript.** { *; }
-keepclasseswithmembernames,includedescriptorclasses class * { native <methods>; }

####################
# Web Call Related #
####################

#Retrofit2
# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep class * extends com.gbgroup.idscan.bento.enterprice.Response { *; }

#okhttp3
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

-keep interface com.gbgroup.idscan.bento.enterprice.** {
    @retrofit2.http.* <methods>;
}
-keep class com.gbgroup.idscan.bento.enterprice.** {
    @retrofit2.http.* <methods>;
}
-keep class com.gbgroup.idscan.bento.enterprice.** {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.gbgroup.idscan.bento.enterprice.** {
    @com.google.gson.annotations.Expose <fields>;
}

##################
# ReadId Related #
##################

# keep the logger
-keep enum nl.innovalor.logging.data.** { *; }
-keep enum nl.innovalor.logging.data.**$** { *; }
-keepclassmembers class nl.innovalor.logging.data.** { <fields>; }
# keep the IsoDepCardService constructors
-keep,includedescriptorclasses class net.sf.scuba.smartcards.IsoDepCardService { public <init>(***); }
# keep the verifier
-keep,includedescriptorclasses class nl.innovalor.mrtd.verifier.MRTDVerifierImpl { *; }
# keep the data model
-keep,includedescriptorclasses class nl.innovalor.mrtd.model.** { private <fields>; <methods>; }
# keep the cert constructors
-keep,includedescriptorclasses class nl.innovalor.cert.** { public <init>(***); }
# keep bouncy castle
-keep,includedescriptorclasses class org.bouncycastle.** { *; }
# keep the scuba country
-keepclassmembers class * extends net.sf.scuba.data.Country { *; }
# keep JJ2000 decoder, only needed in combination with DexGuard
-keep,includedescriptorclasses class jj2000.j2k.** { *; }

#################
# Ozone Related #
#################

-keep,includedescriptorclasses class org.bouncycastle.** { *; }
-keep,includedescriptorclasses class net.sf.scuba.smartcards.IsoDepCardService { public <init>(***); }
-keep,includedescriptorclasses class jj2000.j2k.** { *; }

-dontwarn java.applet.Applet
-dontwarn java.awt.BorderLayout
-dontwarn java.awt.Canvas
-dontwarn java.awt.Color
-dontwarn java.awt.Component
-dontwarn java.awt.Container
-dontwarn java.awt.Cursor
-dontwarn java.awt.Dimension
-dontwarn java.awt.Frame
-dontwarn java.awt.Image
-dontwarn java.awt.Insets
-dontwarn java.awt.LayoutManager
-dontwarn java.awt.Point
-dontwarn java.awt.Scrollbar
-dontwarn java.awt.Toolkit
-dontwarn java.awt.event.KeyAdapter
-dontwarn java.awt.event.KeyListener
-dontwarn java.awt.event.MouseAdapter
-dontwarn java.awt.event.MouseListener
-dontwarn java.awt.event.MouseMotionListener
-dontwarn java.awt.event.WindowAdapter
-dontwarn java.awt.event.WindowListener
-dontwarn java.awt.image.ColorModel
-dontwarn java.awt.image.ImageObserver
-dontwarn java.awt.image.ImageProducer

#################
# Tink (AndroidX Security / EncryptedSharedPreferences)
#################
-dontwarn com.google.errorprone.annotations.**

#################
# Moshi (reflection-based adapter — codegen disabled, see CLAUDE.md)
#################
# Keep JsonClass-annotated models and their constructors/fields for reflection.
-keepclasseswithmembers @com.squareup.moshi.JsonClass class * { <init>(...); }
-keepclassmembers @com.squareup.moshi.JsonClass class * { <fields>; <init>(...); }
-keep @com.squareup.moshi.JsonClass class *

# Belt-and-braces: keep the whole IVS model package since all DTOs live there.
-keep class com.gbg.smartcapture.bigmagic.ivs.** { *; }
-keepclassmembers class com.gbg.smartcapture.bigmagic.ivs.** { *; }

# Kotlin metadata is required by Moshi's KotlinJsonAdapterFactory.
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-keepclassmembers class kotlin.Metadata { <methods>; }
-dontwarn kotlin.reflect.jvm.internal.**

# Moshi internals.
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}
