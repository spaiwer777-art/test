package com.example.calorietracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.AccentColor
import com.example.calorietracker.data.SettingsRepository
import com.example.calorietracker.data.ThemeMode
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

    val dynamicColor: StateFlow<Boolean> = repo.dynamicColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setDailyGoal(value: Double) {
        viewModelScope.launch { repo.setDailyGoal(value) }
    }

    fun setGroqApiKey(value: String) {
        viewModelScope.launch { repo.setGroqApiKey(value) }
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
