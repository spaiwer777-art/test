package com.example.calorietracker.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import android.net.Uri
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.calorietracker.ui.components.mealForCurrentTime
import java.time.LocalDate
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
    const val RECIPES = "recipes"
    const val STATS = "stats"
    const val MORE = "more"
    const val SETTINGS = "settings"
    const val PLAN = "plan"
    const val CALCULATORS = "calculators"
    const val CALCULATOR = "calc/{type}"
    const val ADD_FOOD = "add_food/{day}/{meal}"
    const val SCAN = "scan/{day}/{meal}"
    const val AI_ADD = "ai_add/{day}/{meal}?text={text}"
    const val FOOD = "food/{foodId}/{day}/{meal}"
    const val RECIPE = "recipe/{recipeId}/{day}/{meal}"
    const val RECIPE_EDIT = "recipe_edit?recipeId={recipeId}"
    const val DIETS = "diets"
    const val DIET = "diet/{dietId}"
    const val DIET_EDIT = "diet_edit?dietId={dietId}"
    const val ACCOUNT = "account"
    const val ONBOARDING = "onboarding"
    const val PROFILE = "profile"
    const val WORLD_RECIPE = "world_recipe/{mealId}"

    fun addFood(day: Long, meal: MealType) = "add_food/$day/${meal.name}"
    fun scan(day: Long, meal: MealType) = "scan/$day/${meal.name}"
    fun aiAdd(day: Long, meal: MealType, text: String = "") = "ai_add/$day/${meal.name}?text=${Uri.encode(text)}"
    fun food(id: Long, day: Long, meal: MealType) = "food/$id/$day/${meal.name}"
    fun recipe(id: Long, day: Long, meal: MealType) = "recipe/$id/$day/${meal.name}"
    fun calculator(type: CalcType) = "calc/${type.name}"
    fun recipeEdit(id: Long = 0) = "recipe_edit?recipeId=$id"
    fun diet(id: Long) = "diet/$id"
    fun dietEdit(id: Long = 0) = "diet_edit?dietId=$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

@Suppress("DEPRECATION") // MenuBook: the AutoMirrored variant isn't needed for an LTR-only app.
private val tabs = listOf(
    Tab(Routes.DIARY, "Дневник", Icons.Outlined.MenuBook, Icons.Filled.MenuBook),
    Tab(Routes.RECIPES, "Рецепты", Icons.Outlined.RestaurantMenu, Icons.Filled.RestaurantMenu),
    Tab(Routes.STATS, "Статистика", Icons.Outlined.BarChart, Icons.Filled.BarChart),
    Tab(Routes.MORE, "Ещё", Icons.Outlined.GridView, Icons.Filled.GridView)
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
fun AppNavHost(startOnboarding: Boolean = false) {
    val navController: NavHostController = rememberNavController()
    val bottomBar: @Composable () -> Unit = { AppBottomBar(navController) }
    val back: () -> Unit = { navController.popBackStack() }
    val today = LocalDate.now().toEpochDay()

    fun mealArg(value: String?) = MealType.entries.firstOrNull { it.name == value } ?: MealType.BREAKFAST
    fun NavBackStackEntry.day() = arguments?.getLong("day") ?: today
    fun NavBackStackEntry.meal() = mealArg(arguments?.getString("meal"))

    /** Returns to the diary after something was logged, dropping the add flow from the stack. */
    val backToDiary: () -> Unit = { navController.popBackStack(Routes.DIARY, inclusive = false) }
    val push = AnimatedContentTransitionScope.SlideDirection.Start
    val pop = AnimatedContentTransitionScope.SlideDirection.End

    NavHost(
        navController = navController,
        startDestination = if (startOnboarding) Routes.ONBOARDING else Routes.DIARY,
        // Tabs cross-fade; detail screens slide in from the side.
        enterTransition = { fadeIn(tween(250)) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(250)) },
        popExitTransition = { fadeOut(tween(200)) }
    ) {
        fun detail(
            route: String,
            arguments: List<NamedNavArgument> = emptyList(),
            content: @Composable (NavBackStackEntry) -> Unit
        ) = composable(
            route, arguments = arguments,
            enterTransition = { slideIntoContainer(push, tween(320)) + fadeIn(tween(320)) },
            popExitTransition = { slideOutOfContainer(pop, tween(280)) + fadeOut(tween(280)) }
        ) { content(it) }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onDone = { navController.navigate(Routes.DIARY) { popUpTo(Routes.ONBOARDING) { inclusive = true } } },
                onRestore = {
                    navController.navigate(Routes.DIARY) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    navController.navigate(Routes.ACCOUNT)
                }
            )
        }
        composable(Routes.DIARY) {
            DiaryScreen(
                onAddFood = { day, meal -> navController.navigate(Routes.addFood(day, meal)) },
                onScanBarcode = { day, meal -> navController.navigate(Routes.scan(day, meal)) },
                onAiQuickAdd = { day, meal -> navController.navigate(Routes.aiAdd(day, meal)) },
                onOpenFood = { id, day, meal -> navController.navigate(Routes.food(id, day, meal)) },
                onOpenRecipe = { id, day, meal -> navController.navigate(Routes.recipe(id, day, meal)) },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                bottomBar = bottomBar
            )
        }
        composable(Routes.RECIPES) {
            RecipesScreen(
                onOpen = { navController.navigate(Routes.recipe(it, today, mealForCurrentTime())) },
                onOpenWorld = { navController.navigate("world_recipe/$it") },
                onCreate = { navController.navigate(Routes.recipeEdit()) },
                bottomBar = bottomBar
            )
        }
        composable(Routes.STATS) { StatsScreen(bottomBar = bottomBar) }
        composable(Routes.MORE) {
            MoreScreen(
                onPlan = { navController.navigate(Routes.PLAN) },
                onDiets = { navController.navigate(Routes.DIETS) },
                onAccount = { navController.navigate(Routes.ACCOUNT) },
                onCalculators = { navController.navigate(Routes.CALCULATORS) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onProfile = { navController.navigate(Routes.PROFILE) },
                bottomBar = bottomBar
            )
        }

        detail(Routes.SETTINGS) { SettingsScreen(onBack = back, onOpenProfile = { navController.navigate(Routes.PROFILE) }) }
        detail(Routes.PROFILE) { ProfileScreen(onBack = back) }
        detail(Routes.PLAN) {
            PlanScreen(
                onOpenRecipe = { navController.navigate(Routes.recipe(it, today, mealForCurrentTime())) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onBack = back
            )
        }
        detail(Routes.CALCULATORS) { CalculatorsScreen(onOpen = { navController.navigate(Routes.calculator(it)) }, onBack = back) }
        detail(Routes.CALCULATOR, listOf(navArgument("type") { type = NavType.StringType })) { entry ->
            val type = CalcType.entries.firstOrNull { it.name == entry.arguments?.getString("type") } ?: CalcType.CALORIES
            CalculatorScreen(type, onBack = back)
        }
        detail(Routes.ADD_FOOD, dayMealArgs) { entry ->
            val day = entry.day()
            val meal = entry.meal()
            AddFoodScreen(
                onOpenFood = { navController.navigate(Routes.food(it, day, meal)) },
                onScan = { navController.navigate(Routes.scan(day, meal)) },
                onAi = { navController.navigate(Routes.aiAdd(day, meal, it)) },
                onBack = back
            )
        }
        detail(Routes.SCAN, dayMealArgs) { entry ->
            val day = entry.day()
            val meal = entry.meal()
            BarcodeScannerScreen(
                onFound = { id ->
                    navController.navigate(Routes.food(id, day, meal)) { popUpTo(Routes.SCAN) { inclusive = true } }
                },
                onSearchByName = {
                    navController.navigate(Routes.addFood(day, meal)) { popUpTo(Routes.SCAN) { inclusive = true } }
                },
                onBack = back
            )
        }
        detail(
            Routes.AI_ADD,
            dayMealArgs + navArgument("text") { type = NavType.StringType; defaultValue = "" }
        ) { entry ->
            AiQuickAddScreen(
                epochDay = entry.day(),
                initialMeal = entry.meal(),
                initialText = entry.arguments?.getString("text").orEmpty(),
                onDone = backToDiary,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onBack = back
            )
        }
        detail(Routes.FOOD, dayMealArgs + navArgument("foodId") { type = NavType.LongType }) { entry ->
            FoodDetailsScreen(epochDay = entry.day(), initialMeal = entry.meal(), onAdded = backToDiary, onBack = back)
        }
        detail(Routes.RECIPE, dayMealArgs + navArgument("recipeId") { type = NavType.LongType }) { entry ->
            RecipeDetailsScreen(
                epochDay = entry.day(), initialMeal = entry.meal(), onAdded = backToDiary,
                onEdit = { navController.navigate(Routes.recipeEdit(it)) }, onBack = back
            )
        }
        detail(Routes.RECIPE_EDIT, listOf(navArgument("recipeId") { type = NavType.LongType; defaultValue = 0L })) { entry ->
            val sourceId = entry.arguments?.getLong("recipeId") ?: 0L
            RecipeEditorScreen(
                onSaved = { id ->
                    // Edited in place: the details screen underneath already shows it.
                    if (id == sourceId) back()
                    else navController.navigate(Routes.recipe(id, today, mealForCurrentTime())) {
                        popUpTo(Routes.RECIPE_EDIT) { inclusive = true }
                    }
                },
                onBack = back
            )
        }
        detail(Routes.DIETS) {
            DietsScreen(
                onOpen = { navController.navigate(Routes.diet(it)) },
                onCreate = { navController.navigate(Routes.dietEdit()) },
                onBack = back
            )
        }
        detail(Routes.DIET, listOf(navArgument("dietId") { type = NavType.LongType })) {
            DietDetailsScreen(onEdit = { navController.navigate(Routes.dietEdit(it)) }, onBack = back)
        }
        detail(Routes.DIET_EDIT, listOf(navArgument("dietId") { type = NavType.LongType; defaultValue = 0L })) { entry ->
            val sourceId = entry.arguments?.getLong("dietId") ?: 0L
            DietEditorScreen(
                onSaved = { id ->
                    if (id == sourceId) back()
                    else navController.navigate(Routes.diet(id)) { popUpTo(Routes.DIET_EDIT) { inclusive = true } }
                },
                onBack = back
            )
        }
        detail(Routes.ACCOUNT) { AccountScreen(onBack = back) }
        detail(Routes.WORLD_RECIPE, listOf(navArgument("mealId") { type = NavType.StringType })) {
            WorldRecipeScreen(
                onSaved = { id -> navController.navigate(Routes.recipe(id, today, mealForCurrentTime())) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onBack = back
            )
        }
    }
}
