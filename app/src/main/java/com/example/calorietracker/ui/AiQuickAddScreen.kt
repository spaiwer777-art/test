package com.example.calorietracker.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.data.MealType
import com.example.calorietracker.ui.components.MealSelector
import com.example.calorietracker.ui.theme.Macro
import com.example.calorietracker.ui.theme.macroColor
import com.example.calorietracker.viewmodel.AiQuickAddViewModel
import com.example.calorietracker.viewmodel.AiState
import com.example.calorietracker.viewmodel.DiaryViewModel
import kotlin.math.roundToInt

private val examples = listOf(
    "Гречка 200 г с курицей",
    "Капучино и круассан",
    "Омлет из 2 яиц и тост с маслом",
    "Тарелка борща со сметаной"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiQuickAddScreen(
    epochDay: Long,
    initialMeal: MealType,
    onDone: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    val vm: AiQuickAddViewModel = viewModel()
    val diaryVm: DiaryViewModel = viewModel()
    val state by vm.state.collectAsState()

    var description by remember { mutableStateOf("") }
    var meal by remember { mutableStateOf(initialMeal) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ИИ: быстрый ввод") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text("Опиши, что съел, обычными словами", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("например: омлет из 2 яиц и тост с маслом") },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                examples.forEach { ex -> AssistChip(onClick = { description = ex }, label = { Text(ex) }) }
            }
            Spacer(Modifier.height(16.dp))
            Text("Приём пищи", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            MealSelector(selected = meal, onSelect = { meal = it })
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { vm.estimate(description) },
                enabled = description.isNotBlank() && state !is AiState.Loading,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Оценить с ИИ")
            }

            Spacer(Modifier.height(24.dp))

            AnimatedContent(
                targetState = state,
                transitionSpec = { (fadeIn() + slideInVertically { it / 4 } + scaleIn(initialScale = 0.95f)) togetherWith fadeOut() },
                contentKey = { it::class },
                label = "ai-state"
            ) { s ->
                when (s) {
                    AiState.Idle -> Spacer(Modifier.height(1.dp))
                    AiState.Loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text("Считаю калории…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    is AiState.NoApiKey -> MessageCard("Сначала добавь бесплатный API-ключ Groq в настройках.") {
                        TextButton(onClick = onOpenSettings) { Text("Открыть настройки") }
                    }
                    is AiState.Error -> MessageCard(s.message) {
                        TextButton(onClick = { vm.estimate(description) }) { Text("Повторить") }
                    }
                    is AiState.Success -> {
                        val e = s.estimate
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Text(e.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${e.calories.roundToInt()} ккал",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    MacroValue("Белки", e.protein, macroColor(Macro.PROTEIN))
                                    MacroValue("Жиры", e.fat, macroColor(Macro.FAT))
                                    MacroValue("Углеводы", e.carbs, macroColor(Macro.CARBS))
                                }
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        diaryVm.addPrecomputedEntry(e.name, e.calories, e.protein, e.fat, e.carbs, meal, epochDay)
                                        vm.reset()
                                        onDone()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Добавить в дневник") }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MacroValue(label: String, grams: Double, color: androidx.compose.ui.graphics.Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text("${grams.roundToInt()} г", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun MessageCard(text: String, action: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text, style = MaterialTheme.typography.bodyMedium)
                action()
            }
        }
    }
}
