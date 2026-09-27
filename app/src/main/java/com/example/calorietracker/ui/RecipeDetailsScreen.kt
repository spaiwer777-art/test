package com.example.calorietracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.data.MealType
import com.example.calorietracker.data.Recipe
import com.example.calorietracker.data.RecipeIngredient
import com.example.calorietracker.ui.components.AnimatedNumber
import com.example.calorietracker.ui.components.IconBadge
import com.example.calorietracker.ui.components.MacroDonut
import com.example.calorietracker.ui.components.MealSelector
import com.example.calorietracker.ui.components.Pill
import com.example.calorietracker.ui.components.SectionCard
import com.example.calorietracker.ui.components.formatGrams
import com.example.calorietracker.viewmodel.DiaryViewModel
import com.example.calorietracker.viewmodel.RecipeDetailsViewModel
import kotlin.math.roundToInt

@Composable
fun RecipeDetailsScreen(epochDay: Long, initialMeal: MealType, onAdded: () -> Unit, onBack: () -> Unit) {
    val vm: RecipeDetailsViewModel = viewModel()
    val diaryVm: DiaryViewModel = viewModel()
    val recipe by vm.recipe.collectAsState()
    val ingredients by vm.ingredients.collectAsState()
    val goal by vm.dailyGoal.collectAsState()
    val r = recipe
    if (r == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    RecipeDetailsContent(
        recipe = r,
        ingredients = ingredients,
        dailyGoal = goal,
        initialMeal = initialMeal,
        onAdd = { portions, meal ->
            val gramsPerServing = ingredients.sumOf { it.grams } / r.servings
            diaryVm.addPrecomputedEntry(
                r.name, r.caloriesPerServing * portions, r.proteinPerServing * portions,
                r.fatPerServing * portions, r.carbsPerServing * portions, meal, epochDay,
                grams = (gramsPerServing * portions).roundToInt().toDouble(), recipeId = r.id
            )
            onAdded()
        },
        onDelete = if (!r.isBuiltin) ({ vm.delete(onBack) }) else null,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecipeDetailsContent(
    recipe: Recipe,
    ingredients: List<RecipeIngredient>,
    dailyGoal: Double,
    initialMeal: MealType,
    onAdd: (portions: Double, meal: MealType) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit
) {
    var portions by remember { mutableDoubleStateOf(1.0) }
    var meal by remember { mutableStateOf(initialMeal) }
    var cookServings by remember(recipe.id) { mutableIntStateOf(recipe.servings) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Рецепт") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
                actions = { if (onDelete != null) IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Удалить рецепт") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(recipeCategoryIcon(recipe.category), recipeCategoryColor(), 56.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(recipe.name, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Pill(recipe.category)
                            Icon(Icons.Outlined.Schedule, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${recipe.minutes} мин", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${recipe.caloriesPerServing.roundToInt()}", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.width(6.dp))
                    Text("ккал в порции", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
                }
                Spacer(Modifier.height(16.dp))
                MacroDonut(recipe.proteinPerServing, recipe.fatPerServing, recipe.carbsPerServing)
            }

            SectionCard(title = "Записать в дневник") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Порций", style = MaterialTheme.typography.labelLarge)
                    listOf(0.5, 1.0, 1.5, 2.0).forEach { p ->
                        FilterChip(selected = portions == p, onClick = { portions = p }, label = { Text(formatGrams(p)) })
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedNumber((recipe.caloriesPerServing * portions).roundToInt(), MaterialTheme.typography.headlineMedium, MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "ккал · ${(recipe.caloriesPerServing * portions / dailyGoal * 100).roundToInt()}% нормы",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                MealSelector(selected = meal, onSelect = { meal = it })
                Spacer(Modifier.height(16.dp))
                Button(onClick = { onAdd(portions, meal) }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Добавить в дневник") }
            }

            SectionCard(
                title = "Ингредиенты",
                subtitle = "на $cookServings порц.",
                action = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(onClick = { if (cookServings > 1) cookServings-- }) { Icon(Icons.Filled.Remove, "Меньше порций") }
                        Text("$cookServings", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 8.dp))
                        FilledTonalIconButton(onClick = { cookServings++ }) { Icon(Icons.Filled.Add, "Больше порций") }
                    }
                }
            ) {
                val scale = cookServings.toDouble() / recipe.servings
                ingredients.forEach { ing ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        Spacer(Modifier.width(12.dp))
                        Text(ing.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text("${formatGrams(ing.grams * scale)} г", style = MaterialTheme.typography.labelLarge)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            val steps = recipe.steps.lines().filter { it.isNotBlank() }
            if (steps.isNotEmpty()) {
                SectionCard(title = "Приготовление") {
                    steps.forEachIndexed { i, step ->
                        Row(Modifier.padding(vertical = 8.dp)) {
                            Box(
                                Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(step, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 3.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
