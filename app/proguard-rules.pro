# Add project specific ProGuard rules here.
-keep class com.farego.app.db.entity.** { *; }
-keep class com.farego.app.network.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class com.google.gson.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.android.libraries.places.** { *; }
