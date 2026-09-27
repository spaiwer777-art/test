package com.example.calorietracker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.example.calorietracker.ui.components.AppTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.data.Food
import com.example.calorietracker.data.FoodSource
import com.example.calorietracker.data.MealType
import com.example.calorietracker.ui.components.AnimatedNumber
import com.example.calorietracker.ui.components.DietWarning
import com.example.calorietracker.ui.components.MacroDonut
import com.example.calorietracker.ui.components.MealSelector
import com.example.calorietracker.ui.components.NutriScoreBadge
import com.example.calorietracker.ui.components.Pill
import com.example.calorietracker.ui.components.SectionCard
import com.example.calorietracker.ui.components.ThinBar
import com.example.calorietracker.ui.components.formatGrams
import com.example.calorietracker.ui.components.toNumberOrNull
import com.example.calorietracker.viewmodel.DiaryViewModel
import com.example.calorietracker.viewmodel.FoodDetailsViewModel
import kotlin.math.roundToInt

@Composable
fun FoodDetailsScreen(epochDay: Long, initialMeal: MealType, onAdded: () -> Unit, onBack: () -> Unit) {
    val vm: FoodDetailsViewModel = viewModel()
    val diaryVm: DiaryViewModel = viewModel()
    val food by vm.food.collectAsState()
    val goal by vm.dailyGoal.collectAsState()
    val conflict by vm.dietConflict.collectAsState()
    val f = food
    if (f == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    FoodDetailsContent(
        food = f,
        dailyGoal = goal,
        initialMeal = initialMeal,
        onAdd = { grams, meal ->
            diaryVm.addEntry(
                f.name, grams, f.caloriesPer100g, f.proteinPer100g, f.fatPer100g, f.carbsPer100g, meal, epochDay, foodId = f.id
            )
            onAdded()
        },
        onDelete = if (!f.source.isReference) ({ vm.delete(onBack) }) else null,
        onBack = onBack,
        dietConflict = conflict
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FoodDetailsContent(
    food: Food,
    dailyGoal: Double,
    initialMeal: MealType,
    onAdd: (grams: Double, meal: MealType) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit,
    dietConflict: String? = null
) {
    var gramsText by remember(food.id) { mutableStateOf(formatGrams(food.servingGrams ?: 100.0)) }
    var meal by remember { mutableStateOf(initialMeal) }
    val grams = gramsText.toNumberOrNull() ?: 0.0
    val k = grams / 100

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Карточка продукта") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
                },
                actions = {
                    if (onDelete != null) IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Удалить продукт")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard {
                Text(food.name, style = MaterialTheme.typography.headlineSmall)
                food.brand?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill(food.source.label)
                    food.category?.let { Pill(it, MaterialTheme.colorScheme.secondary) }
                }
                food.nutriScore?.let {
                    Spacer(Modifier.height(10.dp))
                    NutriScoreBadge(it)
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${food.caloriesPer100g.roundToInt()}", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.width(6.dp))
                    Text("ккал на 100 г", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
                }
                Spacer(Modifier.height(16.dp))
                MacroDonut(food.proteinPer100g, food.fatPer100g, food.carbsPer100g)
            }

            if (dietConflict != null) DietWarning("Не рекомендуется в диете $dietConflict")

            SectionCard(title = "Порция") {
                AppTextField(
                    value = gramsText,
                    onValueChange = { gramsText = it },
                    label = { Text("Граммы") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                val presets = buildList {
                    food.servingGrams?.let { add("${food.servingLabel ?: "порция"} · ${formatGrams(it)} г" to it) }
                    listOf(50.0, 100.0, 150.0, 200.0).forEach { add("${it.roundToInt()} г" to it) }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { (label, g) ->
                        FilterChip(selected = grams == g, onClick = { gramsText = formatGrams(g) }, label = { Text(label) })
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedNumber((food.caloriesPer100g * k).roundToInt(), MaterialTheme.typography.headlineMedium, MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text("ккал", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
                }
                Text(
                    "Б ${formatGrams(food.proteinPer100g * k)} г · Ж ${formatGrams(food.fatPer100g * k)} г · У ${formatGrams(food.carbsPer100g * k)} г",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                val share = if (dailyGoal > 0) (food.caloriesPer100g * k / dailyGoal).toFloat() else 0f
                val animatedShare by animateFloatAsState(share, label = "share")
                ThinBar(animatedShare, MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${(share * 100).roundToInt()}% дневной нормы (${dailyGoal.roundToInt()} ккал)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                MealSelector(selected = meal, onSelect = { meal = it })
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onAdd(grams, meal) },
                    enabled = grams > 0,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Добавить в дневник") }
            }

            SectionCard(title = "Пищевая ценность", subtitle = "на 100 г и на порцию ${formatGrams(grams)} г") {
                NutrientRow("Калорийность", food.caloriesPer100g, k, "ккал", bold = true)
                NutrientRow("Белки", food.proteinPer100g, k, "г", bold = true)
                NutrientRow("Жиры", food.fatPer100g, k, "г", bold = true)
                food.saturatedFatPer100g?.let { NutrientRow("в т.ч. насыщенные", it, k, "г", indent = true) }
                NutrientRow("Углеводы", food.carbsPer100g, k, "г", bold = true)
                food.sugarPer100g?.let { NutrientRow("в т.ч. сахара", it, k, "г", indent = true) }
                food.fiberPer100g?.let { NutrientRow("Клетчатка", it, k, "г") }
                food.saltPer100g?.let { NutrientRow("Соль", it, k, "г") }
                if (food.source.isReference) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Средние справочные значения. Для конкретной марки точнее данные с упаковки.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NutrientRow(label: String, per100: Double, factor: Double, unit: String, bold: Boolean = false, indent: Boolean = false) {
    val style = if (bold) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = if (indent) 12.dp else 0.dp)) {
        Text(label, style = style, modifier = Modifier.weight(1f))
        Text("${formatGrams(per100)} $unit", style = style, modifier = Modifier.width(80.dp))
        Text("${formatGrams(per100 * factor)} $unit", style = style, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(80.dp))
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
