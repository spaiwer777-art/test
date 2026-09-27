package com.example.calorietracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel()
    val goal by vm.dailyGoal.collectAsState()
    val apiKey by vm.groqApiKey.collectAsState()

    var goalText by remember(goal) { mutableStateOf(goal.toInt().toString()) }
    var apiKeyText by remember(apiKey) { mutableStateOf(apiKey) }

    Scaffold(topBar = { TopAppBar(title = { Text("Настройки") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("Дневная цель по калориям", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = goalText,
                onValueChange = { goalText = it },
                label = { Text("Ккал в день") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.setDailyGoal(goalText.toDoubleOrNull() ?: 2000.0) }) {
                Text("Сохранить цель")
            }

            Spacer(Modifier.height(32.dp))
            Text("ИИ (быстрый ввод текстом)", style = MaterialTheme.typography.titleMedium)
            Text(
                "Нужен бесплатный API-ключ Groq (console.groq.com) — используется только для оценки калорий по описанию блюда.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = apiKeyText,
                onValueChange = { apiKeyText = it },
                label = { Text("Groq API-ключ") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.setGroqApiKey(apiKeyText) }) {
                Text("Сохранить ключ")
            }
        }
    }
}
