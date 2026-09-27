package com.example.calorietracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.AiIngredient
import com.example.calorietracker.data.AiMealEstimate
import com.example.calorietracker.graph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed class AiState {
    object Idle : AiState()
    object Loading : AiState()
    data class Success(val estimate: AiMealEstimate) : AiState()
    data class Error(val message: String) : AiState()
    object NoApiKey : AiState()
}

/** Human-readable message for a failed Groq call. */
fun aiErrorMessage(e: Exception): String = when (e) {
    is HttpException -> when (e.code()) {
        401 -> "Groq не принял API-ключ. Проверь ключ в настройках."
        413 -> "Запрос слишком большой для лимитов Groq. Сократи его."
        429 -> "Достигнут лимит бесплатного Groq. Подожди минуту (или до завтра, если исчерпан дневной лимит)."
        else -> "Сервер Groq ответил ошибкой ${e.code()}. Попробуй позже."
    }
    is IOException -> "Нет подключения к интернету."
    else -> "ИИ вернул непонятный ответ. Попробуй ещё раз или переформулируй."
}

class AiQuickAddViewModel(application: Application) : AndroidViewModel(application) {
    private val graph = application.graph

    private val _state = MutableStateFlow<AiState>(AiState.Idle)
    val state: StateFlow<AiState> = _state.asStateFlow()

    fun estimate(description: String) {
        viewModelScope.launch {
            val apiKey = graph.settings.groqApiKey.first()
            if (apiKey.isBlank()) {
                _state.value = AiState.NoApiKey
                return@launch
            }
            _state.value = AiState.Loading
            _state.value = try {
                AiState.Success(graph.ai.estimateMeal(description, apiKey))
            } catch (e: Exception) {
                AiState.Error(aiErrorMessage(e))
            }
        }
    }

    /** Lets the user correct an ingredient's weight; totals recompute from per-100g values. */
    fun setGrams(index: Int, grams: Double) = updateItems { items ->
        items.mapIndexed { i, it -> if (i == index) it.copy(grams = grams.coerceAtLeast(0.0)) else it }
    }

    fun removeItem(index: Int) = updateItems { items -> items.filterIndexed { i, _ -> i != index } }

    private fun updateItems(transform: (List<AiIngredient>) -> List<AiIngredient>) {
        val s = _state.value as? AiState.Success ?: return
        _state.value = AiState.Success(s.estimate.copy(items = transform(s.estimate.items)))
    }

    fun reset() {
        _state.value = AiState.Idle
    }
}
