package com.example.calorietracker.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.example.calorietracker.ui.components.AppTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.data.Food
import com.example.calorietracker.data.FoodSource
import com.example.calorietracker.ui.components.toNumberOrNull
import com.example.calorietracker.viewmodel.OnlineState
import com.example.calorietracker.viewmodel.SearchFilter
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import com.example.calorietracker.viewmodel.SearchViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(
    onOpenFood: (Long) -> Unit,
    onScan: () -> Unit,
    onAi: (String) -> Unit,
    onBack: () -> Unit
) {
    val vm: SearchViewModel = viewModel()
    val query by vm.query.collectAsState()
    val results by vm.results.collectAsState()
    val online by vm.online.collectAsState()
    val filter by vm.filter.collectAsState()
    var showManualDialog by remember { mutableStateOf(false) }

    val mine = results.filter { !it.source.isReference }
    val ru = results.filter { it.source == FoodSource.BUILTIN }
    val usda = results.filter { it.source == FoodSource.USDA }

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
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                AppTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    placeholder = { Text("Например: майонез, творог 5%, гречка") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Очистить")
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { vm.searchOnline() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = onScan, label = { Text("Штрихкод") }, leadingIcon = { Icon(Icons.Filled.QrCodeScanner, null) })
                    AssistChip(onClick = { onAi(query) }, label = { Text("Описать ИИ") }, leadingIcon = { Icon(Icons.Filled.AutoAwesome, null) })
                    AssistChip(onClick = { showManualDialog = true }, label = { Text("Вручную") }, leadingIcon = { Icon(Icons.Filled.Add, null) })
                }
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchFilter.entries.forEach { f ->
                        FilterChip(selected = filter == f, onClick = { vm.setFilter(f) }, label = { Text(f.label) })
                    }
                }
            }

            fun section(title: String, list: List<Food>, key: String) {
                if (list.isEmpty()) return
                item(key = "h$key") { SectionHeader(if (query.isBlank()) title else "$title · ${list.size}") }
                items(list, key = { "$key${it.id}" }) { food -> FoodRow(food, Modifier.animateItem()) { onOpenFood(food.id) } }
            }
            section("Мои продукты", mine, "m")
            section("Справочник РФ", ru, "r")
            section("USDA — база Минсельхоза США (перевод)", usda, "u")

            if (filter.sources.isNotEmpty() && results.isEmpty() && query.isNotBlank()) {
                item { Text("В офлайн-базах ничего не нашлось.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp)) }
            }

            if (filter.online && query.trim().length >= 2) {
                item { SectionHeader("Магазинные продукты · Open Food Facts") }
                item {
                    AnimatedContent(online, transitionSpec = { fadeIn() togetherWith fadeOut() }, contentKey = { it::class }, label = "online") { s ->
                        when (s) {
                            OnlineState.Idle -> OutlinedButton(onClick = vm::searchOnline, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.Public, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Искать «${query.trim()}» онлайн")
                            }
                            OnlineState.Loading -> Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                                CircularProgressIndicator()
                            }
                            is OnlineState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
                            is OnlineState.Results -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (s.foods.isEmpty()) {
                                    Text("Онлайн ничего не нашлось.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
                                }
                                s.foods.forEach { food -> FoodRow(food) { vm.openOnline(food, onOpenFood) } }
                            }
                        }
                    }
                }
                item {
                    TextButton(onClick = { onAi(query) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.AutoAwesome, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Не нашли? Посчитать «${query.trim()}» с ИИ")
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        ManualFoodDialog(
            initialName = query,
            onDismiss = { showManualDialog = false },
            onSave = { name, cal, prot, fat, carbs ->
                vm.saveManualFood(name, cal, prot, fat, carbs, onOpenFood)
                showManualDialog = false
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp, start = 4.dp)
    )
}

@Composable
private fun FoodRow(food: Food, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            Modifier.clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(food.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val sub = listOfNotNull(
                    food.brand,
                    "Б ${food.proteinPer100g.roundToInt()} · Ж ${food.fatPer100g.roundToInt()} · У ${food.carbsPer100g.roundToInt()}"
                ).joinToString(" · ")
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("${food.caloriesPer100g.roundToInt()}", style = MaterialTheme.typography.titleMedium)
                Text("ккал/100 г", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ManualFoodDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (name: String, calories: Double, protein: Double, fat: Double, carbs: Double) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    val numberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый продукт (на 100 г)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, singleLine = true)
                AppTextField(value = calories, onValueChange = { calories = it }, label = { Text("Калории") }, keyboardOptions = numberKeyboard, singleLine = true)
                AppTextField(value = protein, onValueChange = { protein = it }, label = { Text("Белки, г") }, keyboardOptions = numberKeyboard, singleLine = true)
                AppTextField(value = fat, onValueChange = { fat = it }, label = { Text("Жиры, г") }, keyboardOptions = numberKeyboard, singleLine = true)
                AppTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Углеводы, г") }, keyboardOptions = numberKeyboard, singleLine = true)
                Spacer(Modifier.height(4.dp))
                Text("Значения — с упаковки, на 100 г продукта.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
