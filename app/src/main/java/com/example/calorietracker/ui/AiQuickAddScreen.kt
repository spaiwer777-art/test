package com.example.calorietracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.data.MealType
import com.example.calorietracker.viewmodel.AiQuickAddViewModel
import com.example.calorietracker.viewmodel.AiState
import com.example.calorietracker.viewmodel.DiaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiQuickAddScreen(onDone: () -> Unit, onOpenSettings: () -> Unit) {
    val vm: AiQuickAddViewModel = viewModel()
    val diaryVm: DiaryViewModel = viewModel()
    val state by vm.state.collectAsState()

    var description by remember { mutableStateOf("") }
    var meal by remember { mutableStateOf(MealType.BREAKFAST) }
    var expanded by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("ИИ: быстрый ввод") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("Опиши, что съел, обычными словами:", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("например: омлет из 2 яиц и тост с маслом") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
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
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.estimate(description) },
                enabled = description.isNotBlank() && state !is AiState.Loading
            ) {
                Text("Оценить с ИИ")
            }

            Spacer(Modifier.height(24.dp))

            when (val s = state) {
                AiState.Idle -> Unit
                AiState.Loading -> CircularProgressIndicator()
                is AiState.NoApiKey -> Column {
                    Text("Сначала добавь бесплатный API-ключ Groq в настройках.")
                    TextButton(onClick = onOpenSettings) { Text("Открыть настройки") }
                }
                is AiState.Error -> Text("Ошибка: ${s.message}")
                is AiState.Success -> {
                    val e = s.estimate
                    Card {
                        Column(Modifier.padding(16.dp)) {
                            Text(e.name, style = MaterialTheme.typography.titleMedium)
                            Text("${e.calories.toInt()} ккал")
                            Text("Б: ${e.protein.toInt()}г  Ж: ${e.fat.toInt()}г  У: ${e.carbs.toInt()}г")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                diaryVm.addPrecomputedEntry(e.name, e.calories, e.protein, e.fat, e.carbs, meal)
                                vm.reset()
                                onDone()
                            }) {
                                Text("Добавить в дневник")
                            }
                        }
                    }
                }
            }
        }
    }
}
