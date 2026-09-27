package com.example.calorietracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.SettingsRepository
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

    fun setDailyGoal(value: Double) {
        viewModelScope.launch { repo.setDailyGoal(value) }
    }

    fun setGroqApiKey(value: String) {
        viewModelScope.launch { repo.setGroqApiKey(value) }
    }
}
