package com.example.calorietracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.data.Food
import com.example.calorietracker.data.MealType
import com.example.calorietracker.ui.components.MealSelector
import com.example.calorietracker.viewmodel.AddFoodViewModel
import com.example.calorietracker.viewmodel.DiaryViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(epochDay: Long, initialMeal: MealType, onDone: () -> Unit, onBack: () -> Unit) {
    val vm: AddFoodViewModel = viewModel()
    val diaryVm: DiaryViewModel = viewModel()
    val query by vm.query.collectAsState()
    val results by vm.results.collectAsState()

    var showManualDialog by remember { mutableStateOf(false) }
    var selectedFood by remember { mutableStateOf<Food?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Добавить продукт") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                placeholder = { Text("Поиск в моих продуктах") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = { showManualDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Новый продукт вручную")
            }
            Spacer(Modifier.height(12.dp))
            if (results.isEmpty()) {
                Text(
                    if (query.isBlank()) "Здесь появятся твои продукты: добавленные вручную и найденные по штрихкоду."
                    else "Ничего не нашлось. Добавь продукт вручную.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(results, key = { it.id }) { food ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).animateItem(),
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        ListItem(
                            headlineContent = { Text(food.name) },
                            supportingContent = {
                                Text(
                                    "${food.caloriesPer100g.roundToInt()} ккал · Б ${food.proteinPer100g.roundToInt()} · " +
                                        "Ж ${food.fatPer100g.roundToInt()} · У ${food.carbsPer100g.roundToInt()} на 100 г"
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.clickable { selectedFood = food }
                        )
                    }
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
            initialMeal = initialMeal,
            onDismiss = { selectedFood = null },
            onConfirm = { grams, meal ->
                diaryVm.addEntry(
                    foodName = food.name,
                    grams = grams,
                    calsPer100 = food.caloriesPer100g,
                    proteinPer100 = food.proteinPer100g,
                    fatPer100 = food.fatPer100g,
                    carbsPer100 = food.carbsPer100g,
                    meal = meal,
                    epochDay = epochDay
                )
                selectedFood = null
                onDone()
            }
        )
    }
}

private fun String.toNumberOrNull(): Double? = replace(',', '.').toDoubleOrNull()

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
    val numberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый продукт (на 100 г)") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, singleLine = true)
                OutlinedTextField(value = calories, onValueChange = { calories = it }, label = { Text("Калории") }, keyboardOptions = numberKeyboard, singleLine = true)
                OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Белки, г") }, keyboardOptions = numberKeyboard, singleLine = true)
                OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Жиры, г") }, keyboardOptions = numberKeyboard, singleLine = true)
                OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Углеводы, г") }, keyboardOptions = numberKeyboard, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    name.ifBlank { "Без названия" },
                    calories.toNumberOrNull() ?: 0.0,
                    protein.toNumberOrNull() ?: 0.0,
                    fat.toNumberOrNull() ?: 0.0,
                    carbs.toNumberOrNull() ?: 0.0
                )
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

/** Grams + meal picker with a live calorie preview; shared by search and barcode screens. */
@Composable
fun PortionDialog(
    food: Food,
    initialMeal: MealType,
    onDismiss: () -> Unit,
    onConfirm: (grams: Double, meal: MealType) -> Unit
) {
    var grams by remember { mutableStateOf("100") }
    var meal by remember { mutableStateOf(initialMeal) }
    val g = grams.toNumberOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(food.name) },
        text = {
            Column {
                OutlinedTextField(
                    value = grams,
                    onValueChange = { grams = it },
                    label = { Text("Порция, г") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${(food.caloriesPer100g * g / 100).roundToInt()} ккал",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Б ${(food.proteinPer100g * g / 100).roundToInt()} г · Ж ${(food.fatPer100g * g / 100).roundToInt()} г · " +
                        "У ${(food.carbsPer100g * g / 100).roundToInt()} г",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                MealSelector(selected = meal, onSelect = { meal = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(g.takeIf { it > 0 } ?: 100.0, meal) }) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
