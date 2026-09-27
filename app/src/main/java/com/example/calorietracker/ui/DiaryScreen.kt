package com.example.calorietracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.data.DiaryEntry
import com.example.calorietracker.data.MealType
import com.example.calorietracker.viewmodel.DiaryViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onAddFood: () -> Unit,
    onScanBarcode: () -> Unit,
    onAiQuickAdd: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val viewModel: DiaryViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Дневник питания") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Настройки")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallFloatingActionButton(onClick = onAiQuickAdd) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = "ИИ ввод")
                }
                SmallFloatingActionButton(onClick = onScanBarcode) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = "Сканировать штрихкод")
                }
                FloatingActionButton(onClick = onAddFood) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить продукт")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            SummaryCard(
                calories = state.totalCalories,
                goal = state.dailyGoal,
                protein = state.totalProtein,
                fat = state.totalFat,
                carbs = state.totalCarbs
            )
            if (state.entries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Сегодня пока пусто. Добавь первый приём пищи!", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(MealType.entries) { meal ->
                        val mealEntries = state.entries.filter { it.mealType == meal }
                        if (mealEntries.isNotEmpty()) {
                            MealSection(meal, mealEntries, onDelete = { viewModel.deleteEntry(it) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(calories: Double, goal: Double, protein: Double, fat: Double, carbs: Double) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("${calories.roundToInt()} ккал", style = MaterialTheme.typography.titleLarge)
                    Text("из ${goal.roundToInt()} ккал цели", style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    val remaining = (goal - calories).roundToInt()
                    Text(
                        if (remaining >= 0) "Осталось: $remaining" else "Превышено: ${-remaining}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (calories / goal).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                MacroBadge("Белки", protein)
                MacroBadge("Жиры", fat)
                MacroBadge("Углеводы", carbs)
            }
        }
    }
}

@Composable
private fun MacroBadge(label: String, grams: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${grams.roundToInt()} г", style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

fun MealType.label(): String = when (this) {
    MealType.BREAKFAST -> "Завтрак"
    MealType.LUNCH -> "Обед"
    MealType.DINNER -> "Ужин"
    MealType.SNACK -> "Перекус"
}

@Composable
private fun MealSection(meal: MealType, entries: List<DiaryEntry>, onDelete: (Long) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(meal.label(), style = MaterialTheme.typography.titleMedium)
        entries.forEach { entry ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(entry.foodName, style = MaterialTheme.typography.bodyLarge)
                    Text("${entry.calories.roundToInt()} ккал", style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = { onDelete(entry.id) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                }
            }
        }
    }
}
