package com.example.calorietracker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Accent colors the user can pick for the app chrome (not for chart data). */
enum class AccentColor { GREEN, TEAL, BLUE, VIOLET, PINK, ORANGE }

/**
 * Small local key-value store for user settings: daily calorie goal, look and
 * feel, and the user's own Groq API key for the AI quick-add feature. Nothing
 * here ever leaves the phone except the direct request the user triggers to Groq.
 */
class SettingsRepository(private val context: Context) {
    private val dailyGoalKey = doublePreferencesKey("daily_calorie_goal")
    private val groqApiKeyKey = stringPreferencesKey("groq_api_key")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val accentKey = stringPreferencesKey("accent_color")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")

    val dailyGoal: Flow<Double> = context.dataStore.data.map { it[dailyGoalKey] ?: 2000.0 }
    val groqApiKey: Flow<String> = context.dataStore.data.map { it[groqApiKeyKey] ?: "" }
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[themeModeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }
    val accent: Flow<AccentColor> = context.dataStore.data.map { prefs ->
        prefs[accentKey]?.let { runCatching { AccentColor.valueOf(it) }.getOrNull() } ?: AccentColor.GREEN
    }
    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[dynamicColorKey] ?: false }

    suspend fun setDailyGoal(value: Double) {
        context.dataStore.edit { it[dailyGoalKey] = value }
    }

    suspend fun setGroqApiKey(value: String) {
        context.dataStore.edit { it[groqApiKeyKey] = value.trim() }
    }

    suspend fun setThemeMode(value: ThemeMode) {
        context.dataStore.edit { it[themeModeKey] = value.name }
    }

    suspend fun setAccent(value: AccentColor) {
        context.dataStore.edit { it[accentKey] = value.name }
    }

    suspend fun setDynamicColor(value: Boolean) {
        context.dataStore.edit { it[dynamicColorKey] = value }
    }
}
