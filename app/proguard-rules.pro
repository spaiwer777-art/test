# Add project specific ProGuard rules here.

# Gson maps JSON to these network models by field name via reflection.
-keep class com.example.calorietracker.network.** { *; }

# Assets, AI answers and saved meal plans are parsed with Gson.
-keep class com.example.calorietracker.data.json.** { *; }

# Gson generic types (TypeToken subclasses) need their signatures at runtime.
-keepattributes Signature
-keep class * extends com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
