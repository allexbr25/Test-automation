# Keep Retrofit / Gson models and API interfaces
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep class br.com.hosb.glpi.domain.model.** { *; }
-keep class br.com.hosb.glpi.data.remote.** { *; }

# Strip debug logs in production builds for security
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
