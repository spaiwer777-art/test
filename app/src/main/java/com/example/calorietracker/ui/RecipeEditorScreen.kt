package com.example.calorietracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.data.Food
import com.example.calorietracker.ui.components.SectionCard
import com.example.calorietracker.ui.components.formatGrams
import com.example.calorietracker.ui.components.toNumberOrNull
import com.example.calorietracker.viewmodel.RECIPE_CATEGORIES
import com.example.calorietracker.viewmodel.RecipeEditorViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditorScreen(onSaved: (Long) -> Unit, onBack: () -> Unit) {
    val vm: RecipeEditorViewModel = viewModel()
    val name by vm.name.collectAsState()
    val category by vm.category.collectAsState()
    val servings by vm.servings.collectAsState()
    val minutes by vm.minutes.collectAsState()
    val steps by vm.steps.collectAsState()
    val ingredients by vm.ingredients.collectAsState()
    var picking by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новый рецепт") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard(title = "Основное") {
                OutlinedTextField(name, { vm.name.value = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RECIPE_CATEGORIES.forEach { c ->
                        FilterChip(selected = category == c, onClick = { vm.category.value = c }, label = { Text(c) })
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Порций", servings.toString(), Modifier.weight(1f)) { v -> v.toNumberOrNull()?.let { vm.servings.value = it.roundToInt().coerceIn(1, 50) } }
                    NumberField("Минут", minutes.toString(), Modifier.weight(1f)) { v -> v.toNumberOrNull()?.let { vm.minutes.value = it.roundToInt().coerceIn(0, 1440) } }
                }
            }

            SectionCard(
                title = "Ингредиенты",
                subtitle = if (ingredients.isEmpty()) "Добавь продукты из базы" else {
                    val s = servings.coerceAtLeast(1)
                    "Порция: ${(ingredients.sumOf { it.calories } / s).roundToInt()} ккал · " +
                        "Б ${(ingredients.sumOf { it.protein } / s).roundToInt()} · Ж ${(ingredients.sumOf { it.fat } / s).roundToInt()} · " +
                        "У ${(ingredients.sumOf { it.carbs } / s).roundToInt()}"
                }
            ) {
                ingredients.forEachIndexed { i, ing ->
                    var text by remember(ing.name, i) { mutableStateOf(formatGrams(ing.grams)) }
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(ing.name, style = MaterialTheme.typography.bodyLarge)
                            Text("${ing.calories.roundToInt()} ккал", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedTextField(
                            value = text,
                            onValueChange = { v -> text = v; v.toNumberOrNull()?.let { vm.setIngredientGrams(i, it) } },
                            suffix = { Text("г") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(100.dp)
                        )
                        IconButton(onClick = { vm.removeIngredient(i) }) { Icon(Icons.Filled.Close, "Убрать") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить ингредиент")
                }
            }

            SectionCard(title = "Приготовление", subtitle = "Каждый шаг — с новой строки") {
                OutlinedTextField(
                    value = steps,
                    onValueChange = { vm.steps.value = it },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = { vm.save(onSaved) },
                enabled = name.isNotBlank() && ingredients.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Сохранить рецепт") }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (picking) {
        IngredientPicker(vm, onDismiss = { picking = false })
    }
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    var text by remember { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; onChange(it) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

@Composable
private fun IngredientPicker(vm: RecipeEditorViewModel, onDismiss: () -> Unit) {
    val query by vm.foodQuery.collectAsState()
    val results by vm.foodResults.collectAsState()
    var chosen by remember { mutableStateOf<Food?>(null) }
    var grams by remember { mutableStateOf("100") }
    val food = chosen

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(food?.name ?: "Ингредиент") },
        text = {
            if (food == null) {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { vm.foodQuery.value = it },
                        placeholder = { Text("Поиск продукта") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        singleLine = true
                    )
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(results, key = { it.id }) { f ->
                            Column(Modifier.fillMaxWidth().clickable { chosen = f; grams = formatGrams(f.servingGrams ?: 100.0) }.padding(vertical = 10.dp)) {
                                Text(f.name, style = MaterialTheme.typography.bodyLarge)
                                Text("${f.caloriesPer100g.roundToInt()} ккал/100 г", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = grams,
                    onValueChange = { grams = it },
                    label = { Text("Граммы") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            if (food != null) TextButton(onClick = {
                vm.addIngredient(food, grams.toNumberOrNull() ?: 100.0)
                vm.foodQuery.value = ""
                onDismiss()
            }) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
