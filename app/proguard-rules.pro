# Add project specific ProGuard rules here.

# Gson maps JSON to these network models by field name via reflection.
-keep class com.example.calorietracker.network.** { *; }
