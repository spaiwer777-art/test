package com.example.calorietracker.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Small local key-value store for user settings: daily calorie goal and the
 * user's own Groq API key for the AI quick-add feature. Nothing here ever
 * leaves the phone except the direct request the user triggers to Groq.
 */
class SettingsRepository(private val context: Context) {
    private val dailyGoalKey = doublePreferencesKey("daily_calorie_goal")
    private val groqApiKeyKey = stringPreferencesKey("groq_api_key")

    val dailyGoal: Flow<Double> = context.dataStore.data.map { it[dailyGoalKey] ?: 2000.0 }
    val groqApiKey: Flow<String> = context.dataStore.data.map { it[groqApiKeyKey] ?: "" }

    suspend fun setDailyGoal(value: Double) {
        context.dataStore.edit { it[dailyGoalKey] = value }
    }

    suspend fun setGroqApiKey(value: String) {
        context.dataStore.edit { it[groqApiKeyKey] = value }
    }
}
