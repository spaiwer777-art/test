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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.data.AiIngredient
import com.example.calorietracker.data.AiMealEstimate
import com.example.calorietracker.data.MealType
import com.example.calorietracker.ui.components.AnimatedNumber
import com.example.calorietracker.ui.components.formatGrams
import com.example.calorietracker.ui.components.toNumberOrNull
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
    initialText: String,
    onDone: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    val vm: AiQuickAddViewModel = viewModel()
    val diaryVm: DiaryViewModel = viewModel()
    val state by vm.state.collectAsState()

    var description by remember { mutableStateOf(initialText) }
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
            AppTextField(
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
                    is AiState.Success -> AiResultCard(
                        estimate = s.estimate,
                        onGrams = vm::setGrams,
                        onRemove = vm::removeItem,
                        onAdd = {
                            val e = s.estimate
                            diaryVm.addPrecomputedEntry(
                                e.name, e.items.sumOf { it.calories }, e.items.sumOf { it.protein },
                                e.items.sumOf { it.fat }, e.items.sumOf { it.carbs }, meal, epochDay,
                                grams = e.items.sumOf { it.grams }
                            )
                            vm.reset()
                            onDone()
                        }
                    )
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

@Composable
private fun AiResultCard(
    estimate: AiMealEstimate,
    onGrams: (Int, Double) -> Unit,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit
) {
    val items = estimate.items
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(estimate.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Row(verticalAlignment = Alignment.Bottom) {
                AnimatedNumber(items.sumOf { it.calories }.roundToInt(), MaterialTheme.typography.headlineMedium, MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(6.dp))
                Text("ккал", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MacroValue("Белки", items.sumOf { it.protein }, macroColor(Macro.PROTEIN))
                MacroValue("Жиры", items.sumOf { it.fat }, macroColor(Macro.FAT))
                MacroValue("Углеводы", items.sumOf { it.carbs }, macroColor(Macro.CARBS))
            }
            Spacer(Modifier.height(16.dp))
            Text("Состав — поправь граммы, если нужно", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(4.dp))
            items.forEachIndexed { index, item ->
                IngredientEditRow(item, onGrams = { onGrams(index, it) }, onRemove = { onRemove(index) })
            }
            Spacer(Modifier.height(8.dp))
            val fromBase = items.count { it.baseName != null }
            Text(
                if (fromBase > 0) "✓ $fromBase из ${items.size} ингредиентов посчитаны по справочной базе, остальные — оценка ИИ."
                else "Все значения — оценка ИИ по справочникам. Точность зависит от описания.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAdd, enabled = items.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Добавить в дневник") }
        }
    }
}

@Composable
private fun IngredientEditRow(item: AiIngredient, onGrams: (Double) -> Unit, onRemove: () -> Unit) {
    var text by remember(item.name) { mutableStateOf(formatGrams(item.grams)) }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                (if (item.baseName != null) "✓ база: ${item.baseName} · " else "ИИ · ") + "${item.calories.roundToInt()} ккал",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
            )
        }
        AppTextField(
            value = text,
            onValueChange = { v -> text = v; v.toNumberOrNull()?.let(onGrams) },
            suffix = { Text("г") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(96.dp),
            textStyle = MaterialTheme.typography.bodyMedium
        )
        IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, contentDescription = "Убрать ингредиент") }
    }
}
