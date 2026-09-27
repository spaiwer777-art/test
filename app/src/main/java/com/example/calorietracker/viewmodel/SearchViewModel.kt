package com.example.calorietracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.Food
import com.example.calorietracker.data.FoodSource
import com.example.calorietracker.graph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed class OnlineState {
    object Idle : OnlineState()
    object Loading : OnlineState()
    data class Results(val query: String, val foods: List<Food>) : OnlineState()
    data class Error(val message: String) : OnlineState()
}

/** Which databases the search looks in. */
enum class SearchFilter(val label: String, val hint: String, val sources: List<FoodSource>, val online: Boolean) {
    ALL("Все", "Ищем везде: твои продукты, базовые, мировые и товары из магазинов", FoodSource.entries, true),
    MINE("Мои", "Продукты, которые ты добавил сам или по штрихкоду", listOf(FoodSource.USER, FoodSource.BARCODE, FoodSource.ONLINE), false),
    RU("Базовые", "Справочник обычных продуктов: крупы, мясо, молочка, овощи, фрукты", listOf(FoodSource.BUILTIN), false),
    USDA("Мировые", "Большая база Минсельхоза США (USDA), переведена на русский", listOf(FoodSource.USDA), false),
    OFF("Товары", "Упакованные товары из магазинов с этикеткой — открытая база Open Food Facts", emptyList(), true)
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = application.graph.foods

    val query = MutableStateFlow("")
    val filter = MutableStateFlow(SearchFilter.ALL)

    val results: StateFlow<List<Food>> = combine(query.debounce(150), filter) { q, f -> q to f }
        .flatMapLatest { (q, f) ->
            when {
                f.sources.isEmpty() -> flowOf(emptyList())
                q.isBlank() -> repo.allFoods(f.sources)
                else -> repo.searchFoods(q, f.sources)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _online = MutableStateFlow<OnlineState>(OnlineState.Idle)
    val online: StateFlow<OnlineState> = _online.asStateFlow()
    private var onlineJob: Job? = null

    fun setQuery(value: String) {
        query.value = value
        scheduleOnline()
    }

    fun setFilter(value: SearchFilter) {
        filter.value = value
        scheduleOnline()
    }

    /** Online search runs by itself shortly after typing stops, alongside the local bases. */
    private fun scheduleOnline() {
        onlineJob?.cancel()
        val q = query.value.trim()
        if (!filter.value.online || q.length < 3) {
            _online.value = OnlineState.Idle
            return
        }
        onlineJob = viewModelScope.launch {
            delay(700)
            runOnline(q)
        }
    }

    fun searchOnline() {
        val q = query.value.trim()
        if (q.length < 2) return
        onlineJob?.cancel()
        onlineJob = viewModelScope.launch { runOnline(q) }
    }

    private suspend fun runOnline(q: String) {
        _online.value = OnlineState.Loading
        _online.value = try {
            OnlineState.Results(q, repo.searchOnline(q))
        } catch (e: HttpException) {
            OnlineState.Error("Open Food Facts ответил ошибкой ${e.code()}. Попробуй позже.")
        } catch (e: IOException) {
            OnlineState.Error("Нет подключения к интернету — показаны только офлайн-базы.")
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
