# Standard kotlinx.serialization keep rules (from the library's own recommended R8 config) -
# without these, R8 can strip a field that's only ever accessed by the compiler-generated
# serializer (not read by name in our own code), which fails silently (wrong/null data) rather
# than with a crash - the worst kind of release-only bug to catch after the fact.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class cloud.cyberverse.asterion.data.model.**$$serializer { *; }
-keepclassmembers class cloud.cyberverse.asterion.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class cloud.cyberverse.asterion.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit API interfaces are implemented via dynamic proxy at runtime - keep their method
# signatures (and the generic type info Retrofit inspects) intact.
-keep,allowobfuscation interface cloud.cyberverse.asterion.data.remote.** { *; }

# Clerk's own consumer ProGuard rules (if it ships any) should already cover this, but auth
# breaking silently in a release build is severe enough to warrant an explicit safety net.
-keep class com.clerk.** { *; }
-dontwarn com.clerk.**
