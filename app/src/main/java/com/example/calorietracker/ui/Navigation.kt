package com.example.calorietracker.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object Routes {
    const val DIARY = "diary"
    const val ADD_FOOD = "add_food"
    const val SCAN = "scan"
    const val AI_ADD = "ai_add"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavHost() {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.DIARY) {
        composable(Routes.DIARY) {
            DiaryScreen(
                onAddFood = { navController.navigate(Routes.ADD_FOOD) },
                onScanBarcode = { navController.navigate(Routes.SCAN) },
                onAiQuickAdd = { navController.navigate(Routes.AI_ADD) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.ADD_FOOD) {
            AddFoodScreen(onDone = { navController.popBackStack() })
        }
        composable(Routes.SCAN) {
            BarcodeScannerScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.AI_ADD) {
            AiQuickAddScreen(
                onDone = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
