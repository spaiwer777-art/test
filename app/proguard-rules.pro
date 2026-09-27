# Add project specific ProGuard rules here.

# Gson maps JSON to these network models by field name via reflection.
-keep class com.example.calorietracker.network.** { *; }

# Assets, AI answers and saved meal plans are parsed with Gson.
-keep class com.example.calorietracker.data.json.** { *; }

# Gson generic types (TypeToken subclasses) need their signatures at runtime.
-keepattributes Signature
-keep class * extends com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken

# Cloud session is stored as JSON with Gson.
-keep class com.example.calorietracker.data.cloud.CloudSession { *; }
# Backup JSON contains Room entities; keep their field names stable for Gson.
-keep class com.example.calorietracker.data.Food { *; }
-keep class com.example.calorietracker.data.DiaryEntry { *; }
-keep class com.example.calorietracker.data.Recipe { *; }
-keep class com.example.calorietracker.data.RecipeIngredient { *; }
-keep class com.example.calorietracker.data.Diet { *; }
-keep class com.example.calorietracker.data.WeightEntry { *; }
-keep class com.example.calorietracker.data.WaterEntry { *; }
-keep class com.example.calorietracker.data.MealPlanEntity { *; }
-keep class com.example.calorietracker.data.MealPhoto { *; }
-keep enum com.example.calorietracker.data.** { *; }
