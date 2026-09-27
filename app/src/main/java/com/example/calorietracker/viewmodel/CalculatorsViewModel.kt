package com.example.calorietracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.Profile
import com.example.calorietracker.graph
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class CalculatorsViewModel(application: Application) : AndroidViewModel(application) {
    private val graph = application.graph

    /** Saved profile, with the weight taken from the weight log when there is one. */
    val profile: StateFlow<Profile> = combine(graph.settings.profile, graph.tracking.latestWeight()) { p, w ->
        if (w != null) p.copy(weightKg = w.kg) else p
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Profile())

    fun saveProfile(value: Profile) {
        viewModelScope.launch { graph.settings.setProfile(value) }
    }

    fun setCalorieGoal(kcal: Double) {
        viewModelScope.launch { graph.settings.setDailyGoal(kcal) }
    }

    fun logWeight(kg: Double) {
        viewModelScope.launch { graph.tracking.setWeight(LocalDate.now().toEpochDay(), kg) }
    }
}
