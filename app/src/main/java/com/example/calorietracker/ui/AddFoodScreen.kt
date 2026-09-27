package com.example.calorietracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.data.Food
import com.example.calorietracker.data.MealType
import com.example.calorietracker.viewmodel.AddFoodViewModel
import com.example.calorietracker.viewmodel.DiaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(onDone: () -> Unit) {
    val vm: AddFoodViewModel = viewModel()
    val diaryVm: DiaryViewModel = viewModel()
    val query by vm.query.collectAsState()
    val results by vm.results.collectAsState()

    var showManualDialog by remember { mutableStateOf(false) }
    var selectedFood by remember { mutableStateOf<Food?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Добавить продукт") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                label = { Text("Поиск продукта") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { showManualDialog = true }) {
                Text("Добавить новый продукт вручную")
            }
            LazyColumn(Modifier.weight(1f)) {
                items(results) { food ->
                    ListItem(
                        headlineContent = { Text(food.name) },
                        supportingContent = { Text("${food.caloriesPer100g.toInt()} ккал / 100г") },
                        modifier = Modifier.fillMaxWidth().clickable { selectedFood = food }
                    )
                    HorizontalDividerCompat()
                }
            }
        }
    }

    if (showManualDialog) {
        ManualFoodDialog(
            onDismiss = { showManualDialog = false },
            onSave = { name, cal, prot, fat, carbs ->
                vm.saveManualFood(name, cal, prot, fat, carbs) { food ->
                    selectedFood = food
                }
                showManualDialog = false
            }
        )
    }

    selectedFood?.let { food ->
        PortionDialog(
            food = food,
            onDismiss = { selectedFood = null },
            onConfirm = { grams, meal ->
                diaryVm.addEntry(
                    foodName = food.name,
                    grams = grams,
                    calsPer100 = food.caloriesPer100g,
                    proteinPer100 = food.proteinPer100g,
                    fatPer100 = food.fatPer100g,
                    carbsPer100 = food.carbsPer100g,
                    meal = meal
                )
                selectedFood = null
                onDone()
            }
        )
    }
}

@Composable
private fun HorizontalDividerCompat() {
    HorizontalDivider()
}

@Composable
private fun ManualFoodDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, calories: Double, protein: Double, fat: Double, carbs: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый продукт (на 100 г)") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") })
                OutlinedTextField(value = calories, onValueChange = { calories = it }, label = { Text("Калории") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Белки, г") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Жиры, г") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Углеводы, г") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    name.ifBlank { "Без названия" },
                    calories.toDoubleOrNull() ?: 0.0,
                    protein.toDoubleOrNull() ?: 0.0,
                    fat.toDoubleOrNull() ?: 0.0,
                    carbs.toDoubleOrNull() ?: 0.0
                )
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun PortionDialog(
    food: Food,
    onDismiss: () -> Unit,
    onConfirm: (grams: Double, meal: MealType) -> Unit
) {
    var grams by remember { mutableStateOf("100") }
    var meal by remember { mutableStateOf(MealType.BREAKFAST) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(food.name) },
        text = {
            Column {
                OutlinedTextField(
                    value = grams,
                    onValueChange = { grams = it },
                    label = { Text("Граммы") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(8.dp))
                Box {
                    TextButton(onClick = { expanded = true }) { Text("Приём пищи: ${meal.label()}") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        MealType.entries.forEach {
                            DropdownMenuItem(text = { Text(it.label()) }, onClick = { meal = it; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(grams.toDoubleOrNull() ?: 100.0, meal) }) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
