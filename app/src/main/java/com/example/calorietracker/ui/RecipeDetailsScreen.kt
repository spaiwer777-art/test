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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.text.KeyboardOptions
import com.example.calorietracker.ui.components.AppTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.ui.text.input.KeyboardType
import com.example.calorietracker.ui.components.DietWarning
import com.example.calorietracker.ui.components.toNumberOrNull
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
fun RecipeDetailsScreen(epochDay: Long, initialMeal: MealType, onAdded: () -> Unit, onEdit: (Long) -> Unit, onBack: () -> Unit) {
    val vm: RecipeDetailsViewModel = viewModel()
    val diaryVm: DiaryViewModel = viewModel()
    val recipe by vm.recipe.collectAsState()
    val ingredients by vm.ingredients.collectAsState()
    val goal by vm.dailyGoal.collectAsState()
    val conflict by vm.dietConflict.collectAsState()
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
        dietConflict = conflict,
        onAdd = { portions, meal ->
            // Portions are fractions of the whole dish; grams mode passes grams / dish weight.
            val dishWeight = r.cookedWeight ?: ingredients.sumOf { it.grams }
            diaryVm.addPrecomputedEntry(
                r.name, r.caloriesPerServing * portions, r.proteinPerServing * portions,
                r.fatPerServing * portions, r.carbsPerServing * portions, meal, epochDay,
                grams = (dishWeight / r.servings * portions).roundToInt().toDouble(), recipeId = r.id
            )
            onAdded()
        },
        onEdit = { onEdit(r.id) },
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
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    dietConflict: String? = null
) {
    var portions by remember { mutableDoubleStateOf(1.0) }
    var byGrams by remember { mutableStateOf(false) }
    var gramsText by remember { mutableStateOf("") }
    val dishWeight = recipe.cookedWeight ?: ingredients.sumOf { it.grams }
    val servingWeight = if (recipe.servings > 0) dishWeight / recipe.servings else 0.0
    var meal by remember { mutableStateOf(initialMeal) }
    var cookServings by remember(recipe.id) { mutableIntStateOf(recipe.servings) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Рецепт") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(if (recipe.isBuiltin) Icons.Filled.ContentCopy else Icons.Filled.Edit, if (recipe.isBuiltin) "Сделать свою копию" else "Изменить")
                    }
                    if (onDelete != null) IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Удалить рецепт") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            recipe.imageUrl?.let { url ->
                coil.compose.AsyncImage(
                    model = url,
                    contentDescription = recipe.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
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

            if (dietConflict != null) {
                DietWarning("Не подходит диете $dietConflict")
            }

            SectionCard(title = "Всё блюдо", subtitle = "${recipe.servings} порц. · ${dishWeight.roundToInt()} г${if (recipe.cookedWeight != null) " готового" else " (сумма продуктов)"}") {
                Row(Modifier.fillMaxWidth()) {
                    WholeStat("Всего", "${recipe.totalCalories.roundToInt()} ккал", Modifier.weight(1f))
                    WholeStat("На 100 г", "${(recipe.totalCalories / dishWeight * 100).roundToInt()} ккал", Modifier.weight(1f))
                    WholeStat("Порция", "${servingWeight.roundToInt()} г", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Б ${recipe.totalProtein.roundToInt()} · Ж ${recipe.totalFat.roundToInt()} · У ${recipe.totalCarbs.roundToInt()} г во всём блюде",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard(title = "Записать в дневник") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(selected = !byGrams, onClick = { byGrams = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Порции") }
                    SegmentedButton(selected = byGrams, onClick = { byGrams = true }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Граммы") }
                }
                Spacer(Modifier.height(12.dp))
                if (byGrams) {
                    AppTextField(
                        value = gramsText,
                        onValueChange = { gramsText = it; it.toNumberOrNull()?.let { g -> if (servingWeight > 0) portions = g / servingWeight } },
                        label = { Text("Сколько грамм съел") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Порций", style = MaterialTheme.typography.labelLarge)
                    listOf(0.5, 1.0, 1.5, 2.0).forEach { p ->
                        FilterChip(selected = portions == p, onClick = { portions = p; gramsText = formatGrams(servingWeight * p) }, label = { Text(formatGrams(p)) })
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

@Composable
private fun WholeStat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
