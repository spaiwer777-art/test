package com.example.calorietracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.Food
import com.example.calorietracker.graph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview
import retrofit2.HttpException
import java.io.IOException

sealed class OnlineState {
    object Idle : OnlineState()
    object Loading : OnlineState()
    data class Results(val query: String, val foods: List<Food>) : OnlineState()
    data class Error(val message: String) : OnlineState()
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = application.graph.foods

    val query = MutableStateFlow("")

    val results: StateFlow<List<Food>> = query
        .debounce(150)
        .flatMapLatest { q -> if (q.isBlank()) repo.allFoods() else repo.searchFoods(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _online = MutableStateFlow<OnlineState>(OnlineState.Idle)
    val online: StateFlow<OnlineState> = _online.asStateFlow()
    private var onlineJob: Job? = null

    fun setQuery(value: String) {
        query.value = value
        if (_online.value !is OnlineState.Loading) _online.value = OnlineState.Idle
    }

    fun searchOnline() {
        val q = query.value.trim()
        if (q.length < 2) return
        onlineJob?.cancel()
        _online.value = OnlineState.Loading
        onlineJob = viewModelScope.launch {
            _online.value = try {
                OnlineState.Results(q, repo.searchOnline(q))
            } catch (e: HttpException) {
                OnlineState.Error("Open Food Facts ответил ошибкой ${e.code()}. Попробуй позже.")
            } catch (e: IOException) {
                OnlineState.Error("Нет подключения к интернету.")
            }
        }
    }

    /** Saves an online result so it gets an id and can be opened like any product. */
    fun openOnline(food: Food, onSaved: (Long) -> Unit) {
        viewModelScope.launch { onSaved(repo.saveOnline(food)) }
    }

    fun saveManualFood(name: String, calories: Double, protein: Double, fat: Double, carbs: Double, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            onSaved(repo.saveFood(Food(name = name, caloriesPer100g = calories, proteinPer100g = protein, fatPer100g = fat, carbsPer100g = carbs)))
        }
    }
}
