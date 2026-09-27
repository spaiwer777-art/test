package com.example.calorietracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.calorietracker.data.AccentColor
import com.example.calorietracker.data.SettingsRepository
import com.example.calorietracker.data.ThemeMode
import com.example.calorietracker.ui.AppNavHost
import com.example.calorietracker.ui.theme.CalorieTrackerTheme

class MainActivity : ComponentActivity() {
    /** Leaving the app is a natural moment to back up to the cloud (at most every 15 minutes). */
    override fun onStop() {
        super.onStop()
        val app = application as CalorieApp
        app.appScope.launch {
            if (app.graph.settings.cloudAuto.first()) app.graph.syncUp(minIntervalMs = 15 * 60 * 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Logo splash stays up until we know whether to show onboarding.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = SettingsRepository(applicationContext)
        var onboarded: Boolean? = null
        splash.setKeepOnScreenCondition { onboarded == null }
        lifecycleScope.launch { onboarded = settings.onboarded.first() }
        setContent {
            val themeMode by settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val accent by settings.accent.collectAsState(initial = AccentColor.GREEN)
            val dynamicColor by settings.dynamicColor.collectAsState(initial = false)
            CalorieTrackerTheme(themeMode = themeMode, accent = accent, dynamicColor = dynamicColor) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // Decide the start screen once; later changes of the flag must not rebuild the nav graph.
                    val ready by settings.onboarded.collectAsState(initial = null)
                    val startWith = remember { mutableStateOf<Boolean?>(null) }
                    if (startWith.value == null && ready != null) startWith.value = ready
                    startWith.value?.let { AppNavHost(startOnboarding = !it) }
                }
            }
        }
    }
}
