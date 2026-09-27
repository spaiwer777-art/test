package com.example.calorietracker

import android.app.Application
import com.example.calorietracker.data.AiRepository
import com.example.calorietracker.data.AppDatabase
import com.example.calorietracker.data.BackupManager
import com.example.calorietracker.data.cloud.CloudSync
import kotlinx.coroutines.flow.first
import com.example.calorietracker.data.DiaryRepository
import com.example.calorietracker.data.DietRepository
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
    val diets = DietRepository(db)
    val backup = BackupManager(app, db, settings)
    val cloud = CloudSync()

    /**
     * Uploads a backup if signed in, auto-sync is on and the last upload is older
     * than [minIntervalMs]. Returns an error message or null.
     */
    suspend fun syncUp(minIntervalMs: Long = 0): String? {
        if (!cloud.isConfigured) return "Облако не настроено"
        val session = settings.cloudSession.first() ?: return "Не выполнен вход"
        if (System.currentTimeMillis() - settings.cloudLastSync.first() < minIntervalMs) return null
        return try {
            val fresh = cloud.fresh(session)
            if (fresh != session) settings.setCloudSession(fresh)
            cloud.upload(fresh, backup.toJson(backup.collect().copy(photos = emptyList())))
            settings.setCloudLastSync(System.currentTimeMillis())
            null
        } catch (e: Exception) {
            e.message ?: "Ошибка синхронизации"
        }
    }
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
