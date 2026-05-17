# Proguard rules
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.coworkapp.loopplayer.**$$serializer { *; }
-keepclassmembers class com.coworkapp.loopplayer.** {
    *** Companion;
}
-keepclasseswithmembers class com.coworkapp.loopplayer.** {
    kotlinx.serialization.KSerializer serializer(...);
}
