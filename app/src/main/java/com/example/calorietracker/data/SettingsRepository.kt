package com.example.calorietracker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Accent colors the user can pick for the app chrome (not for chart data). */
enum class AccentColor { GREEN, TEAL, BLUE, VIOLET, PINK, ORANGE }

enum class Sex(val label: String) { MALE("Мужской"), FEMALE("Женский") }

enum class ActivityLevel(val factor: Double, val label: String, val hint: String) {
    SEDENTARY(1.2, "Минимальная", "сидячая работа, без спорта"),
    LIGHT(1.375, "Низкая", "1–3 тренировки в неделю"),
    MODERATE(1.55, "Средняя", "3–5 тренировок в неделю"),
    HIGH(1.725, "Высокая", "6–7 тренировок в неделю"),
    EXTREME(1.9, "Очень высокая", "физический труд + спорт")
}

enum class WeightGoal(val factor: Double, val label: String) {
    LOSE(0.85, "Похудеть"),
    MAINTAIN(1.0, "Поддерживать вес"),
    GAIN(1.10, "Набрать массу")
}

/** Body data used by calculators and the meal planner. */
data class Profile(
    val sex: Sex = Sex.MALE,
    val age: Int = 30,
    val heightCm: Double = 175.0,
    val weightKg: Double = 75.0,
    val activity: ActivityLevel = ActivityLevel.LIGHT,
    val goal: WeightGoal = WeightGoal.MAINTAIN
)

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
    private val seedVersionKey = intPreferencesKey("seed_version")
    private val sexKey = stringPreferencesKey("profile_sex")
    private val ageKey = intPreferencesKey("profile_age")
    private val heightKey = doublePreferencesKey("profile_height")
    private val weightKey = doublePreferencesKey("profile_weight")
    private val activityKey = stringPreferencesKey("profile_activity")
    private val goalKey = stringPreferencesKey("profile_goal")

    val dailyGoal: Flow<Double> = context.dataStore.data.map { it[dailyGoalKey] ?: 2000.0 }
    val groqApiKey: Flow<String> = context.dataStore.data.map { it[groqApiKeyKey] ?: "" }
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[themeModeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }
    val accent: Flow<AccentColor> = context.dataStore.data.map { prefs ->
        prefs[accentKey]?.let { runCatching { AccentColor.valueOf(it) }.getOrNull() } ?: AccentColor.GREEN
    }
    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[dynamicColorKey] ?: false }
    val seedVersion: Flow<Int> = context.dataStore.data.map { it[seedVersionKey] ?: 0 }

    val profile: Flow<Profile> = context.dataStore.data.map { p ->
        val d = Profile()
        Profile(
            sex = p[sexKey]?.let { v -> Sex.entries.firstOrNull { it.name == v } } ?: d.sex,
            age = p[ageKey] ?: d.age,
            heightCm = p[heightKey] ?: d.heightCm,
            weightKg = p[weightKey] ?: d.weightKg,
            activity = p[activityKey]?.let { v -> ActivityLevel.entries.firstOrNull { it.name == v } } ?: d.activity,
            goal = p[goalKey]?.let { v -> WeightGoal.entries.firstOrNull { it.name == v } } ?: d.goal
        )
    }

    suspend fun setProfile(value: Profile) {
        context.dataStore.edit {
            it[sexKey] = value.sex.name
            it[ageKey] = value.age
            it[heightKey] = value.heightCm
            it[weightKey] = value.weightKg
            it[activityKey] = value.activity.name
            it[goalKey] = value.goal.name
        }
    }

    suspend fun setSeedVersion(value: Int) {
        context.dataStore.edit { it[seedVersionKey] = value }
    }

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
