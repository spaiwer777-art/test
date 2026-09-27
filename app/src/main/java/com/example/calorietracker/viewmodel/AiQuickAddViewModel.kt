package com.example.calorietracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.AppDatabase
import com.example.calorietracker.data.FoodRepository
import com.example.calorietracker.data.SettingsRepository
import com.example.calorietracker.network.AiNutritionEstimate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class AiState {
    object Idle : AiState()
    object Loading : AiState()
    data class Success(val estimate: AiNutritionEstimate) : AiState()
    data class Error(val message: String) : AiState()
    object NoApiKey : AiState()
}

class AiQuickAddViewModel(application: Application) : AndroidViewModel(application) {
    private val foodRepo = FoodRepository(AppDatabase.get(application))
    private val settingsRepo = SettingsRepository(application)

    private val _state = MutableStateFlow<AiState>(AiState.Idle)
    val state: StateFlow<AiState> = _state.asStateFlow()

    fun estimate(description: String) {
        viewModelScope.launch {
            val apiKey = settingsRepo.groqApiKey.first()
            if (apiKey.isBlank()) {
                _state.value = AiState.NoApiKey
                return@launch
            }
            _state.value = AiState.Loading
            try {
                val result = foodRepo.estimateWithAi(description, apiKey)
                _state.value = AiState.Success(result)
            } catch (e: Exception) {
                _state.value = AiState.Error(e.message ?: "Не удалось получить оценку от ИИ")
            }
        }
    }

    fun reset() {
        _state.value = AiState.Idle
    }
}
