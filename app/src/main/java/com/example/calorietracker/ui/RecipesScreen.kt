package com.example.calorietracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.data.Recipe
import com.example.calorietracker.ui.components.EmptyState
import com.example.calorietracker.ui.components.IconBadge
import com.example.calorietracker.ui.components.Pill
import com.example.calorietracker.ui.theme.Macro
import com.example.calorietracker.ui.theme.macroColor
import com.example.calorietracker.viewmodel.RECIPE_CATEGORIES
import com.example.calorietracker.viewmodel.RecipesViewModel
import kotlin.math.roundToInt

fun recipeCategoryIcon(category: String): ImageVector = when (category) {
    "Завтраки" -> Icons.Filled.EggAlt
    "Супы" -> Icons.Filled.SoupKitchen
    "Салаты" -> Icons.Filled.Restaurant
    "Перекусы" -> Icons.Filled.Cookie
    else -> Icons.Filled.RamenDining
}

/** Category tint borrowed from the primary scheme so it follows the chosen accent. */
@Composable
fun recipeCategoryColor(): Color = MaterialTheme.colorScheme.primary

@Composable
fun RecipesScreen(onOpen: (Long) -> Unit, onCreate: () -> Unit, bottomBar: @Composable () -> Unit) {
    val vm: RecipesViewModel = viewModel()
    val recipes by vm.recipes.collectAsState()
    val query by vm.query.collectAsState()
    val category by vm.category.collectAsState()
    RecipesContent(recipes, query, category, { vm.query.value = it }, { vm.category.value = it }, onOpen, onCreate, bottomBar)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecipesContent(
    recipes: List<Recipe>,
    query: String,
    category: String?,
    onQuery: (String) -> Unit,
    onCategory: (String?) -> Unit,
    onOpen: (Long) -> Unit,
    onCreate: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Рецепты", style = MaterialTheme.typography.headlineSmall) }) },
        bottomBar = bottomBar,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onCreate, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Свой рецепт") })
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQuery,
                    placeholder = { Text("Поиск рецептов") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Filled.Clear, "Очистить") } },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = category == null, onClick = { onCategory(null) }, label = { Text("Все") })
                    RECIPE_CATEGORIES.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { onCategory(if (category == c) null else c) },
                            label = { Text(c) },
                            leadingIcon = { Icon(recipeCategoryIcon(c), null, Modifier.size(18.dp)) }
                        )
                    }
                }
            }
            if (recipes.isEmpty()) item { EmptyState(Icons.Filled.Search, "Рецептов не найдено") }
            items(recipes, key = { it.id }) { r -> RecipeCard(r, Modifier.animateItem()) { onOpen(r.id) } }
        }
    }
}

@Composable
private fun RecipeCard(recipe: Recipe, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(recipeCategoryIcon(recipe.category), recipeCategoryColor(), size = 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(recipe.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${recipe.minutes} мин · ${recipe.servings} порц.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!recipe.isBuiltin) {
                        Spacer(Modifier.width(8.dp))
                        Pill("Мой")
                    }
                }
                Spacer(Modifier.height(8.dp))
                MacroSplitLine(recipe.proteinPerServing, recipe.fatPerServing, recipe.carbsPerServing, Modifier.width(140.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("${recipe.caloriesPerServing.roundToInt()}", style = MaterialTheme.typography.titleLarge)
                Text("ккал/порц.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** 4dp stacked bar of the energy split; exact numbers are on the details screen. */
@Composable
fun MacroSplitLine(protein: Double, fat: Double, carbs: Double, modifier: Modifier = Modifier) {
    val parts = listOf(carbs * 4 to macroColor(Macro.CARBS), protein * 4 to macroColor(Macro.PROTEIN), fat * 9 to macroColor(Macro.FAT))
    val total = parts.sumOf { it.first }
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier.height(4.dp).clip(MaterialTheme.shapes.small)) {
        drawRect(track)
        if (total <= 0) return@Canvas
        var x = 0f
        val gap = 2.dp.toPx()
        parts.forEach { (e, color) ->
            val w = size.width * (e / total).toFloat()
            if (w > gap) drawRect(color, Offset(x, 0f), Size(w - gap, size.height))
            x += w
        }
    }
}
