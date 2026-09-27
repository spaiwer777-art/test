package com.example.calorietracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.calorietracker.ui.components.IconBadge
import com.example.calorietracker.ui.components.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onPlan: () -> Unit,
    onDiets: () -> Unit,
    onAccount: () -> Unit,
    onCalculators: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit = {},
    bottomBar: @Composable () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Ещё", style = MaterialTheme.typography.headlineSmall) }) },
        bottomBar = bottomBar
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val accent = MaterialTheme.colorScheme.primary
            MoreItem(Icons.Filled.Tune, "Параметры и нормы", "Норма калорий и воды, вес, рост, активность, цель", onProfile)
            MoreItem(Icons.Filled.CloudSync, "Аккаунт и резервная копия", "Вход, сохранение прогресса и перенос на другой телефон", onAccount)
            MoreItem(Icons.Filled.Eco, "Диеты", "Готовые диеты и конструктор своей: БЖУ, калории, правила", onDiets)
            MoreItem(Icons.Filled.EventNote, "Рацион", "План питания на 1–7 дней: с ИИ или из рецептов", onPlan)
            MoreItem(Icons.Filled.Calculate, "Калькуляторы", "Норма калорий, ИМТ, вода, БЖУ, % жира, идеальный вес, тренировки", onCalculators)
            MoreItem(Icons.Filled.Settings, "Настройки", "Тема, цвета, ключ Groq", onSettings)
            SectionCard(title = "Откуда данные") {
                Row {
                    Icon(Icons.Filled.Info, null, tint = accent)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Справочная база — средние значения по таблицам химического состава (Скурихин) и USDA. " +
                            "Магазинные продукты и штрихкоды — Open Food Facts. ИИ-оценки — Groq; " +
                            "ингредиенты, найденные в справочной базе, пересчитываются по ней.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
