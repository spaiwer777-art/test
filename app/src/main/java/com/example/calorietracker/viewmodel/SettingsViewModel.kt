package com.example.calorietracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.AccentColor
import com.example.calorietracker.data.SettingsRepository
import com.example.calorietracker.data.ThemeMode
import com.example.calorietracker.graph
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = SettingsRepository(application)

    val dailyGoal: StateFlow<Double> = repo.dailyGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000.0)

    val groqApiKey: StateFlow<String> = repo.groqApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val themeMode: StateFlow<ThemeMode> = repo.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val accent: StateFlow<AccentColor> = repo.accent
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccentColor.GREEN)

    val macroSplit: StateFlow<com.example.calorietracker.data.MacroSplit> = repo.macroSplit
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.example.calorietracker.data.MacroSplit.DEFAULT)

    val dynamicColor: StateFlow<Boolean> = repo.dynamicColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val appName: StateFlow<String> = repo.appName
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun setAppName(value: String) {
        viewModelScope.launch { repo.setAppName(value) }
    }

    fun setDailyGoal(value: Double) {
        viewModelScope.launch { repo.setDailyGoal(value) }
    }

    fun setGroqApiKey(value: String) {
        viewModelScope.launch { repo.setGroqApiKey(value) }
    }

    /** null = not checked yet; true/false with a message after «Проверить». */
    private val _keyCheck = kotlinx.coroutines.flow.MutableStateFlow<Pair<Boolean, String>?>(null)
    val keyCheck: StateFlow<Pair<Boolean, String>?> = _keyCheck
    private val _checking = kotlinx.coroutines.flow.MutableStateFlow(false)
    val checking: StateFlow<Boolean> = _checking

    fun checkGroqKey(value: String) {
        val key = com.example.calorietracker.data.cleanApiKey(value)
        if (key.isEmpty()) { _keyCheck.value = false to "Сначала вставь ключ."; return }
        viewModelScope.launch {
            _checking.value = true
            repo.setGroqApiKey(key)
            _keyCheck.value = try {
                getApplication<Application>().graph.ai.checkKey(key)
                true to "Ключ работает — ИИ готов."
            } catch (e: Exception) {
                false to aiErrorMessage(e)
            }
            _checking.value = false
        }
    }

    fun setThemeMode(value: ThemeMode) {
        viewModelScope.launch { repo.setThemeMode(value) }
    }

    fun setAccent(value: AccentColor) {
        viewModelScope.launch { repo.setAccent(value) }
    }

    fun setDynamicColor(value: Boolean) {
        viewModelScope.launch { repo.setDynamicColor(value) }
    }
}
