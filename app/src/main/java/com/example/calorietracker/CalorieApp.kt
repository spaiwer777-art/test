package com.example.calorietracker

import android.app.Application
import com.example.calorietracker.data.AiRepository
import com.example.calorietracker.data.AppDatabase
import com.example.calorietracker.data.DiaryRepository
import com.example.calorietracker.data.FoodRepository
import com.example.calorietracker.data.RecipeRepository
import com.example.calorietracker.data.TrackingRepository
import com.example.calorietracker.data.Seeder
import com.example.calorietracker.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Repositories shared by all screens. */
class AppGraph(app: Application) {
    val db = AppDatabase.get(app)
    val settings = SettingsRepository(app)
    val foods = FoodRepository(db)
    val ai = AiRepository(foods)
    val diary = DiaryRepository(db)
    val recipes = RecipeRepository(db)
    val tracking = TrackingRepository(db)
}

val Application.graph: AppGraph get() = (this as CalorieApp).graph

class CalorieApp : Application() {
    /** Outlives screens; for work that must finish even if the user navigates away. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val graph by lazy { AppGraph(this) }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            Seeder.run(this@CalorieApp, graph.db, graph.settings)
        }
    }
}
