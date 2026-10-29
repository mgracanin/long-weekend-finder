# Gson builds these from JSON (API responses and saved plans) by reflection,
# so their field names must survive R8.
-keep class com.longweekend.optimizer.model.** { *; }
-keepclassmembers enum com.longweekend.optimizer.model.** { *; }

# Generic type info for Gson's TypeToken (saved-plans list) and Retrofit's suspend API
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
