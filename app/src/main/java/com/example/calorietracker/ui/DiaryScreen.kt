package com.example.calorietracker.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.data.DiaryEntry
import com.example.calorietracker.data.MealType
import com.example.calorietracker.ui.components.CalorieSummary
import com.example.calorietracker.ui.components.MacroBar
import com.example.calorietracker.ui.components.icon
import com.example.calorietracker.ui.components.label
import com.example.calorietracker.ui.components.mealForCurrentTime
import com.example.calorietracker.ui.theme.Macro
import com.example.calorietracker.ui.theme.macroColor
import com.example.calorietracker.ui.theme.mealColor
import com.example.calorietracker.viewmodel.DiaryUiState
import com.example.calorietracker.viewmodel.DiaryViewModel
import com.example.calorietracker.viewmodel.goalShare
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DiaryScreen(
    onAddFood: (day: Long, meal: MealType) -> Unit,
    onScanBarcode: (day: Long, meal: MealType) -> Unit,
    onAiQuickAdd: (day: Long, meal: MealType) -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val viewModel: DiaryViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    DiaryContent(
        state = state,
        selectedDay = selectedDay,
        onShiftDay = viewModel::shiftDay,
        onToday = viewModel::goToToday,
        onDelete = viewModel::deleteEntry,
        onAddFood = onAddFood,
        onScanBarcode = onScanBarcode,
        onAiQuickAdd = onAiQuickAdd,
        bottomBar = bottomBar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiaryContent(
    state: DiaryUiState,
    selectedDay: Long,
    onShiftDay: (Long) -> Unit,
    onToday: () -> Unit,
    onDelete: (Long) -> Unit,
    onAddFood: (day: Long, meal: MealType) -> Unit,
    onScanBarcode: (day: Long, meal: MealType) -> Unit,
    onAiQuickAdd: (day: Long, meal: MealType) -> Unit,
    bottomBar: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Дневник", style = MaterialTheme.typography.headlineSmall) })
        },
        bottomBar = bottomBar,
        floatingActionButton = {
            AddSpeedDial(
                onAddFood = { onAddFood(selectedDay, mealForCurrentTime()) },
                onScan = { onScanBarcode(selectedDay, mealForCurrentTime()) },
                onAi = { onAiQuickAdd(selectedDay, mealForCurrentTime()) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DaySwitcher(
                    epochDay = selectedDay,
                    onPrev = { onShiftDay(-1) },
                    onNext = { onShiftDay(1) },
                    onToday = onToday
                )
            }
            item { SummaryCard(state) }
            items(MealType.entries, key = { it.name }) { meal ->
                MealCard(
                    meal = meal,
                    entries = state.entries.filter { it.mealType == meal },
                    goal = state.dailyGoal * meal.goalShare,
                    onAdd = { onAddFood(selectedDay, meal) },
                    onDelete = onDelete
                )
            }
        }
    }
}

private val dayFormatter = DateTimeFormatter.ofPattern("EEE, d MMMM", Locale("ru"))

private fun dayLabel(epochDay: Long): String {
    val today = LocalDate.now().toEpochDay()
    return when (epochDay - today) {
        0L -> "Сегодня"
        -1L -> "Вчера"
        1L -> "Завтра"
        else -> LocalDate.ofEpochDay(epochDay).format(dayFormatter).replaceFirstChar { it.uppercase() }
    }
}

@Composable
private fun DaySwitcher(epochDay: Long, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Предыдущий день") }
        AnimatedContent(
            targetState = epochDay,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally { it / 2 * dir } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 2 * dir } + fadeOut())
            },
            modifier = Modifier.weight(1f),
            label = "day"
        ) { day ->
            Text(
                dayLabel(day),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onToday)
                    .padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        IconButton(onClick = onNext) { Icon(Icons.Filled.ChevronRight, contentDescription = "Следующий день") }
    }
}

@Composable
private fun SummaryCard(state: DiaryUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
            CalorieSummary(
                caloriesByMeal = MealType.entries.associateWith { state.caloriesFor(it) },
                goal = state.dailyGoal,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            val goals = state.macroGoals
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MacroBar("Углеводы", state.totalCarbs, goals.carbs, macroColor(Macro.CARBS), Modifier.weight(1f))
                MacroBar("Белки", state.totalProtein, goals.protein, macroColor(Macro.PROTEIN), Modifier.weight(1f))
                MacroBar("Жиры", state.totalFat, goals.fat, macroColor(Macro.FAT), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MealCard(
    meal: MealType,
    entries: List<DiaryEntry>,
    goal: Double,
    onAdd: () -> Unit,
    onDelete: (Long) -> Unit
) {
    val color = mealColor(meal)
    val eaten = entries.sumOf { it.calories }
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(spring(stiffness = 400f)),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(meal.icon, contentDescription = null, tint = color)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(meal.label(), style = MaterialTheme.typography.titleMedium)
                Text(
                    "${eaten.roundToInt()} / ${goal.roundToInt()} ккал",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalIconButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить в «${meal.label()}»")
            }
        }
        if (entries.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            entries.forEach { entry -> EntryRow(entry, onDelete) }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun EntryRow(entry: DiaryEntry, onDelete: (Long) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(entry.foodName, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val details = buildString {
                if (entry.grams > 0) append("${entry.grams.roundToInt()} г · ")
                append("Б ${entry.protein.roundToInt()} · Ж ${entry.fat.roundToInt()} · У ${entry.carbs.roundToInt()}")
            }
            Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("${entry.calories.roundToInt()} ккал", style = MaterialTheme.typography.labelLarge)
        IconButton(onClick = { onDelete(entry.id) }) {
            Icon(Icons.Outlined.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** "+" button that unfolds into search / barcode / AI options. */
@Composable
private fun AddSpeedDial(onAddFood: () -> Unit, onScan: () -> Unit, onAi: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 45f else 0f, spring(dampingRatio = 0.6f), label = "fab")

    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val options = listOf(
            Triple("ИИ: опиши словами", Icons.Filled.AutoAwesome, onAi),
            Triple("Сканер штрихкода", Icons.Filled.QrCodeScanner, onScan),
            Triple("Поиск продукта", Icons.Filled.Search, onAddFood)
        )
        options.forEach { (label, icon, action) ->
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + slideInVertically { it / 2 } + scaleIn(initialScale = 0.6f),
                exit = fadeOut() + slideOutVertically { it / 2 } + scaleOut(targetScale = 0.6f)
            ) {
                SpeedDialItem(label, icon) { expanded = false; action() }
            }
        }
        FloatingActionButton(onClick = { expanded = !expanded }) {
            // "+" turns into "×" by rotating 45°.
            Icon(
                Icons.Filled.Add,
                contentDescription = if (expanded) "Закрыть" else "Добавить",
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

@Composable
private fun SpeedDialItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 2.dp,
            onClick = onClick
        ) {
            Text(label, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(12.dp))
        SmallFloatingActionButton(onClick = onClick, containerColor = MaterialTheme.colorScheme.primaryContainer) {
            Icon(icon, contentDescription = label)
        }
    }
}
