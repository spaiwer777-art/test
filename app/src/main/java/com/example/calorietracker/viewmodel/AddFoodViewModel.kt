package com.example.calorietracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.AppDatabase
import com.example.calorietracker.data.Food
import com.example.calorietracker.data.FoodRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AddFoodViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = FoodRepository(AppDatabase.get(application))

    val query = MutableStateFlow("")

    val results: StateFlow<List<Food>> = query
        .flatMapLatest { q -> if (q.isBlank()) repo.allFoods() else repo.searchFoods(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(value: String) { query.value = value }

    fun saveManualFood(
        name: String,
        calories: Double,
        protein: Double,
        fat: Double,
        carbs: Double,
        onSaved: (Food) -> Unit
    ) {
        viewModelScope.launch {
            val food = Food(
                name = name,
                caloriesPer100g = calories,
                proteinPer100g = protein,
                fatPer100g = fat,
                carbsPer100g = carbs
            )
            val id = repo.saveFood(food)
            onSaved(food.copy(id = id))
        }
    }
}
