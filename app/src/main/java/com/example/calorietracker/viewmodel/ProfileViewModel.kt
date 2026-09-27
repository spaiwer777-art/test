package com.example.calorietracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.Calc
import com.example.calorietracker.data.Profile
import com.example.calorietracker.graph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ProfileUiState(
    val profile: Profile = Profile(),
    val dailyGoal: Double = 2000.0,
    val calorieAuto: Boolean = false,
    val suggestedCalories: Double = 2000.0,
    val dietName: String? = null,
    /** 0 = automatic. */
    val ownWaterMl: Int = 0,
    val suggestedWaterMl: Int = 2000
) {
    val waterGoalMl: Int get() = if (ownWaterMl > 0) ownWaterMl else suggestedWaterMl
}

/** «Параметры и нормы»: body data, calorie norm and water norm in one place; every change saves itself. */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val graph = application.graph
    private val settings = graph.settings

    /** Edited locally so typing is instant; persisted after a short pause. */
    private val profile = MutableStateFlow<Profile?>(null)
    private var saveJob: Job? = null

    private val activeDiet = settings.activeDietId.flatMapLatest { graph.diets.observe(it) }

    init {
        viewModelScope.launch {
            val p = settings.profile.first()
            val w = graph.tracking.latestWeight().first()
            profile.value = p.copy(weightKg = w?.kg ?: p.weightKg)
        }
    }

    val state: StateFlow<ProfileUiState?> = combine(
        profile, settings.dailyGoal, settings.calorieAuto, settings.waterGoalMl, activeDiet
    ) { p, goal, auto, water, diet ->
        p?.let {
            ProfileUiState(
                profile = it, dailyGoal = goal, calorieAuto = auto,
                suggestedCalories = suggestedCalories(it, diet), dietName = diet?.name,
                ownWaterMl = water, suggestedWaterMl = Calc.waterMl(it.weightKg, it.activity)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun update(p: Profile) {
        val old = profile.value
        profile.value = p
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400)
            settings.setProfile(p)
            if (old == null || old.weightKg != p.weightKg) graph.tracking.setWeight(LocalDate.now().toEpochDay(), p.weightKg)
            if (settings.calorieAuto.first()) {
                val diet = activeDiet.first()
                settings.setDailyGoal(suggestedCalories(p, diet))
            }
        }
    }

    fun setCalorieGoal(kcal: Double) {
        viewModelScope.launch {
            settings.setCalorieAuto(false)
            settings.setDailyGoal(kcal)
        }
    }

    fun setCalorieAuto(on: Boolean) {
        viewModelScope.launch {
            settings.setCalorieAuto(on)
            if (on) state.value?.let { settings.setDailyGoal(it.suggestedCalories) }
        }
    }

    fun setWaterGoal(ml: Int) {
        viewModelScope.launch { settings.setWaterGoalMl(ml) }
    }

    override fun onCleared() {
        // Flush a pending edit when the screen closes mid-debounce.
        val p = profile.value
        if (saveJob?.isActive == true && p != null) {
            graph.let { g ->
                (getApplication<Application>() as com.example.calorietracker.CalorieApp).appScope.launch {
                    g.settings.setProfile(p)
                    g.tracking.setWeight(LocalDate.now().toEpochDay(), p.weightKg)
                    if (g.settings.calorieAuto.first()) g.settings.setDailyGoal(suggestedCalories(p, activeDiet.first()))
                }
            }
        }
    }
}
