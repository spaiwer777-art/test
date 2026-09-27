package com.example.calorietracker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    private val activeDietKey = longPreferencesKey("active_diet")
    private val splitKey = stringPreferencesKey("macro_split")
    private val onboardedKey = booleanPreferencesKey("onboarded")
    private val macroGramsKey = stringPreferencesKey("macro_grams")
    private val cloudSessionKey = stringPreferencesKey("cloud_session")
    private val cloudLastSyncKey = longPreferencesKey("cloud_last_sync")
    private val cloudAutoKey = booleanPreferencesKey("cloud_auto")
    private val waterGoalKey = intPreferencesKey("water_goal_ml")
    private val calorieAutoKey = booleanPreferencesKey("calorie_auto")
    private val appNameKey = stringPreferencesKey("app_name")

    val dailyGoal: Flow<Double> = context.dataStore.data.map { it[dailyGoalKey] ?: 2000.0 }
    /** Own daily water goal in ml; 0 = calculate from weight and activity. */
    val waterGoalMl: Flow<Int> = context.dataStore.data.map { it[waterGoalKey] ?: 0 }
    /** Recalculate the calorie goal whenever the profile changes. */
    val calorieAuto: Flow<Boolean> = context.dataStore.data.map { it[calorieAutoKey] ?: false }

    suspend fun setWaterGoalMl(value: Int) {
        context.dataStore.edit { it[waterGoalKey] = value.coerceAtLeast(0) }
    }

    /** Name for the home-screen shortcut; blank = the app's own name. */
    val appName: Flow<String> = context.dataStore.data.map { it[appNameKey] ?: "" }

    suspend fun setAppName(value: String) {
        context.dataStore.edit { it[appNameKey] = value.trim() }
    }

    suspend fun setCalorieAuto(value: Boolean) {
        context.dataStore.edit { it[calorieAutoKey] = value }
    }

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

    /** Active diet id, 0 = none. */
    val activeDietId: Flow<Long> = context.dataStore.data.map { it[activeDietKey] ?: 0L }

    /** Macro split used for daily macro goals: the active diet's, or 20/30/50. */
    val macroSplit: Flow<MacroSplit> = context.dataStore.data.map { prefs ->
        prefs[splitKey]?.split('/')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 3 }
            ?.let { MacroSplit(it[0], it[1], it[2]) } ?: MacroSplit.DEFAULT
    }

    /** Hand-entered macro goals (grams), or null to derive them from the calorie goal and diet. */
    val macroGrams: Flow<MacroGrams?> = context.dataStore.data.map { prefs ->
        prefs[macroGramsKey]?.split('/')?.mapNotNull { it.toDoubleOrNull() }?.takeIf { it.size == 3 }
            ?.let { MacroGrams(it[0], it[1], it[2]) }
    }

    suspend fun setMacroGrams(value: MacroGrams?) {
        context.dataStore.edit {
            if (value == null) it.remove(macroGramsKey) else it[macroGramsKey] = "${value.protein}/${value.fat}/${value.carbs}"
        }
    }

    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[onboardedKey] ?: false }

    suspend fun setActiveDiet(diet: Diet?) {
        context.dataStore.edit {
            it[activeDietKey] = diet?.id ?: 0L
            val split = diet?.split ?: MacroSplit.DEFAULT
            it[splitKey] = "${split.protein}/${split.fat}/${split.carbs}"
        }
    }

    /** Signed-in cloud session, stored as JSON in the app's private DataStore. */
    val cloudSession: Flow<com.example.calorietracker.data.cloud.CloudSession?> = context.dataStore.data.map { prefs ->
        prefs[cloudSessionKey]?.let { runCatching { com.google.gson.Gson().fromJson(it, com.example.calorietracker.data.cloud.CloudSession::class.java) }.getOrNull() }
    }
    val cloudLastSync: Flow<Long> = context.dataStore.data.map { it[cloudLastSyncKey] ?: 0L }
    val cloudAuto: Flow<Boolean> = context.dataStore.data.map { it[cloudAutoKey] ?: true }

    suspend fun setCloudSession(value: com.example.calorietracker.data.cloud.CloudSession?) {
        context.dataStore.edit {
            if (value == null) it.remove(cloudSessionKey) else it[cloudSessionKey] = com.google.gson.Gson().toJson(value)
        }
    }

    suspend fun setCloudLastSync(value: Long) {
        context.dataStore.edit { it[cloudLastSyncKey] = value }
    }

    suspend fun setCloudAuto(value: Boolean) {
        context.dataStore.edit { it[cloudAutoKey] = value }
    }

    suspend fun setOnboarded(value: Boolean) {
        context.dataStore.edit { it[onboardedKey] = value }
    }

    /** Everything worth carrying over to another phone (not the API key or the theme cache). */
    suspend fun snapshot(): Map<String, String> =
        context.dataStore.data.first().asMap()
            .filterKeys { it.name !in setOf("groq_api_key", "seed_version", "onboarded") && !it.name.startsWith("cloud_") }
            .mapKeys { it.key.name }.mapValues { it.value.toString() }

    /** Restores values written by [snapshot]; types are taken from the known keys. */
    suspend fun restore(values: Map<String, String>) {
        context.dataStore.edit { prefs ->
            values["daily_calorie_goal"]?.toDoubleOrNull()?.let { prefs[dailyGoalKey] = it }
            values["theme_mode"]?.let { prefs[themeModeKey] = it }
            values["accent_color"]?.let { prefs[accentKey] = it }
            values["dynamic_color"]?.toBooleanStrictOrNull()?.let { prefs[dynamicColorKey] = it }
            values["profile_sex"]?.let { prefs[sexKey] = it }
            values["profile_age"]?.toIntOrNull()?.let { prefs[ageKey] = it }
            values["profile_height"]?.toDoubleOrNull()?.let { prefs[heightKey] = it }
            values["profile_weight"]?.toDoubleOrNull()?.let { prefs[weightKey] = it }
            values["profile_activity"]?.let { prefs[activityKey] = it }
            values["profile_goal"]?.let { prefs[goalKey] = it }
            values["active_diet"]?.toLongOrNull()?.let { prefs[activeDietKey] = it }
            values["macro_split"]?.let { prefs[splitKey] = it }
            values["macro_grams"]?.let { prefs[macroGramsKey] = it }
            values["water_goal_ml"]?.toIntOrNull()?.let { prefs[waterGoalKey] = it }
            values["calorie_auto"]?.toBooleanStrictOrNull()?.let { prefs[calorieAutoKey] = it }
            values["app_name"]?.let { prefs[appNameKey] = it }
            prefs[onboardedKey] = true
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
