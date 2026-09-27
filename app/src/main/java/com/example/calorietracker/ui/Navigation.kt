package com.example.calorietracker.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.calorietracker.data.MealType

object Routes {
    const val DIARY = "diary"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val ADD_FOOD = "add_food/{day}/{meal}"
    const val SCAN = "scan/{day}/{meal}"
    const val AI_ADD = "ai_add/{day}/{meal}"

    fun addFood(day: Long, meal: MealType) = "add_food/$day/${meal.name}"
    fun scan(day: Long, meal: MealType) = "scan/$day/${meal.name}"
    fun aiAdd(day: Long, meal: MealType) = "ai_add/$day/${meal.name}"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

@Suppress("DEPRECATION") // MenuBook: the AutoMirrored variant isn't needed for an LTR-only app.
private val tabs = listOf(
    Tab(Routes.DIARY, "Дневник", Icons.Outlined.MenuBook, Icons.Filled.MenuBook),
    Tab(Routes.STATS, "Статистика", Icons.Outlined.BarChart, Icons.Filled.BarChart),
    Tab(Routes.SETTINGS, "Настройки", Icons.Outlined.Settings, Icons.Filled.Settings)
)

private val dayMealArgs = listOf(
    navArgument("day") { type = NavType.LongType },
    navArgument("meal") { type = NavType.StringType }
)

@Composable
private fun AppBottomBar(navController: NavHostController) {
    val backStack by navController.currentBackStackEntryAsState()
    BottomTabs(currentRoute = backStack?.destination?.route) { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

@Composable
internal fun BottomTabs(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) onNavigate(tab.route) },
                icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                label = { Text(tab.label) }
            )
        }
    }
}

@Composable
fun AppNavHost() {
    val navController: NavHostController = rememberNavController()
    val bottomBar: @Composable () -> Unit = { AppBottomBar(navController) }
    val back: () -> Unit = { navController.popBackStack() }

    fun mealArg(value: String?) = MealType.entries.firstOrNull { it.name == value } ?: MealType.BREAKFAST
    val push = AnimatedContentTransitionScope.SlideDirection.Start
    val pop = AnimatedContentTransitionScope.SlideDirection.End

    NavHost(
        navController = navController,
        startDestination = Routes.DIARY,
        // Tabs cross-fade; detail screens slide in from the side (set per destination below).
        enterTransition = { fadeIn(tween(250)) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(250)) },
        popExitTransition = { fadeOut(tween(200)) }
    ) {
        composable(Routes.DIARY) {
            DiaryScreen(
                onAddFood = { day, meal -> navController.navigate(Routes.addFood(day, meal)) },
                onScanBarcode = { day, meal -> navController.navigate(Routes.scan(day, meal)) },
                onAiQuickAdd = { day, meal -> navController.navigate(Routes.aiAdd(day, meal)) },
                bottomBar = bottomBar
            )
        }
        composable(Routes.STATS) { StatsScreen(bottomBar = bottomBar) }
        composable(Routes.SETTINGS) { SettingsScreen(bottomBar = bottomBar) }

        val detailEnter: AnimatedContentTransitionScope<*>.() -> androidx.compose.animation.EnterTransition =
            { slideIntoContainer(push, tween(320)) + fadeIn(tween(320)) }
        val detailPopExit: AnimatedContentTransitionScope<*>.() -> androidx.compose.animation.ExitTransition =
            { slideOutOfContainer(pop, tween(280)) + fadeOut(tween(280)) }

        composable(
            Routes.ADD_FOOD, arguments = dayMealArgs,
            enterTransition = { detailEnter() }, popExitTransition = { detailPopExit() }
        ) { entry ->
            AddFoodScreen(
                epochDay = entry.arguments!!.getLong("day"),
                initialMeal = mealArg(entry.arguments?.getString("meal")),
                onDone = back,
                onBack = back
            )
        }
        composable(
            Routes.SCAN, arguments = dayMealArgs,
            enterTransition = { detailEnter() }, popExitTransition = { detailPopExit() }
        ) { entry ->
            BarcodeScannerScreen(
                epochDay = entry.arguments!!.getLong("day"),
                initialMeal = mealArg(entry.arguments?.getString("meal")),
                onDone = back,
                onBack = back
            )
        }
        composable(
            Routes.AI_ADD, arguments = dayMealArgs,
            enterTransition = { detailEnter() }, popExitTransition = { detailPopExit() }
        ) { entry ->
            AiQuickAddScreen(
                epochDay = entry.arguments!!.getLong("day"),
                initialMeal = mealArg(entry.arguments?.getString("meal")),
                onDone = back,
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                    }
                },
                onBack = back
            )
        }
    }
}
