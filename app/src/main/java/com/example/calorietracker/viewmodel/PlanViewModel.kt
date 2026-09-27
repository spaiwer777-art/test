package com.example.calorietracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.MealPlanEntity
import com.example.calorietracker.data.PlanBuilder
import com.example.calorietracker.data.json.MealPlanData
import com.example.calorietracker.graph
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class PlanGenState {
    object Idle : PlanGenState()
    object Loading : PlanGenState()
    object NoApiKey : PlanGenState()
    data class Error(val message: String) : PlanGenState()
}

class PlanViewModel(application: Application) : AndroidViewModel(application) {
    private val graph = application.graph
    private val gson = Gson()

    val plan: StateFlow<MealPlanData?> = graph.tracking.latestPlan()
        .map { entity -> entity?.let { runCatching { gson.fromJson(it.json, MealPlanData::class.java) }.getOrNull() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dailyGoal: StateFlow<Double> = graph.settings.dailyGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000.0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val dietName: StateFlow<String?> = graph.settings.activeDietId
        .flatMapLatest { graph.diets.observe(it) }
        .map { it?.name }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _gen = MutableStateFlow<PlanGenState>(PlanGenState.Idle)
    val gen: StateFlow<PlanGenState> = _gen.asStateFlow()

    fun generateWithAi(days: Int, includeSnack: Boolean, preferences: String) {
        viewModelScope.launch {
            val key = graph.settings.groqApiKey.first()
            if (key.isBlank()) {
                _gen.value = PlanGenState.NoApiKey
                return@launch
            }
            _gen.value = PlanGenState.Loading
            try {
                val goal = graph.settings.dailyGoal.first()
                val macros = MacroGoals.fromCalories(goal, graph.settings.macroSplit.first(), graph.settings.macroGrams.first())
                val diet = graph.diets.get(graph.settings.activeDietId.first())
                save(graph.ai.generatePlan(key, goal, macros.protein, macros.fat, macros.carbs, days, includeSnack, preferences, diet))
                _gen.value = PlanGenState.Idle
            } catch (e: Exception) {
                _gen.value = PlanGenState.Error(aiErrorMessage(e))
            }
        }
    }

    fun generateFromRecipes(days: Int, includeSnack: Boolean) {
        viewModelScope.launch {
            val goal = graph.settings.dailyGoal.first()
            val diet = graph.diets.get(graph.settings.activeDietId.first())
            save(
                PlanBuilder.build(
                    graph.recipes.allOnce(), graph.recipes.totalGrams(), goal, days, includeSnack,
                    diet = diet, ingredients = graph.recipes.ingredientsOnce()
                )
            )
            _gen.value = PlanGenState.Idle
        }
    }

    private suspend fun save(data: MealPlanData) {
        graph.tracking.savePlan(
            MealPlanEntity(createdAt = System.currentTimeMillis(), title = "Рацион на ${data.days.size} дн.", json = gson.toJson(data))
        )
    }
}
