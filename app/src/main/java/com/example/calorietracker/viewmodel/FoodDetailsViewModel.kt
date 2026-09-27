package com.example.calorietracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.Food
import com.example.calorietracker.graph
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FoodDetailsViewModel(application: Application, handle: SavedStateHandle) : AndroidViewModel(application) {
    private val graph = application.graph
    private val foodId: Long = handle["foodId"] ?: 0L

    val food: StateFlow<Food?> = graph.foods.observe(foodId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dailyGoal: StateFlow<Double> = graph.settings.dailyGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000.0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val dietConflict: StateFlow<String?> = kotlinx.coroutines.flow.combine(
        graph.settings.activeDietId.flatMapLatest { graph.diets.observe(it) }, food
    ) { diet, f ->
        if (diet == null || f == null) null
        else com.example.calorietracker.data.DietRules.conflict(diet, f.name)?.let { "«${diet.name}»: $it" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            graph.foods.deleteFood(foodId)
            onDone()
        }
    }
}
