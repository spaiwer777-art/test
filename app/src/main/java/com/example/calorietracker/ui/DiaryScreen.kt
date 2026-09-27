package com.example.calorietracker.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.calorietracker.data.MealPhoto
import com.example.calorietracker.data.PhotoStore
import com.example.calorietracker.ui.components.AnimatedNumber
import com.example.calorietracker.ui.components.IconBadge
import com.example.calorietracker.ui.components.SectionCard
import com.example.calorietracker.ui.components.ThinBar
import com.example.calorietracker.ui.theme.isDarkSurface
import java.io.File
import androidx.compose.material.icons.filled.Close
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import com.example.calorietracker.data.MacroGrams
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    onOpenFood: (foodId: Long, day: Long, meal: MealType) -> Unit,
    onOpenRecipe: (recipeId: Long, day: Long, meal: MealType) -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val viewModel: DiaryViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val water by viewModel.waterMl.collectAsState()
    val waterGoal by viewModel.waterGoalMl.collectAsState()
    val requestPhoto = rememberMealPhotoPicker(viewModel::addPhoto)
    DiaryContent(
        state = state,
        selectedDay = selectedDay,
        photos = photos,
        waterMl = water,
        waterGoalMl = waterGoal,
        onShiftDay = viewModel::shiftDay,
        onToday = viewModel::goToToday,
        onSelectDay = viewModel::selectDay,
        onDelete = viewModel::deleteEntry,
        onSetWater = viewModel::setWater,
        onSetMacros = viewModel::setCustomMacros,
        onRequestPhoto = requestPhoto,
        onDeletePhoto = viewModel::deletePhoto,
        onAddFood = onAddFood,
        onScanBarcode = onScanBarcode,
        onAiQuickAdd = onAiQuickAdd,
        onOpenFood = onOpenFood,
        onOpenRecipe = onOpenRecipe,
        bottomBar = bottomBar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiaryContent(
    state: DiaryUiState,
    selectedDay: Long,
    photos: List<MealPhoto>,
    waterMl: Int,
    waterGoalMl: Int,
    onShiftDay: (Long) -> Unit,
    onToday: () -> Unit,
    onSelectDay: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onSetWater: (Int) -> Unit,
    onSetMacros: (MacroGrams?, Boolean) -> Unit = { _, _ -> },
    onRequestPhoto: (MealType, fromCamera: Boolean) -> Unit,
    onDeletePhoto: (MealPhoto) -> Unit,
    onAddFood: (day: Long, meal: MealType) -> Unit,
    onScanBarcode: (day: Long, meal: MealType) -> Unit,
    onAiQuickAdd: (day: Long, meal: MealType) -> Unit,
    onOpenFood: (foodId: Long, day: Long, meal: MealType) -> Unit,
    onOpenRecipe: (recipeId: Long, day: Long, meal: MealType) -> Unit,
    bottomBar: @Composable () -> Unit
) {
    var sheetEntry by remember { mutableStateOf<DiaryEntry?>(null) }
    var viewedPhoto by remember { mutableStateOf<MealPhoto?>(null) }
    var editMacros by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(greeting(), style = MaterialTheme.typography.headlineSmall)
                        Text(
                            state.dietName?.let { "Диета: $it" } ?: "Цель ${state.dailyGoal.roundToInt()} ккал",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = { if (state.streak > 0) StreakChip(state.streak) }
            )
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
                WeekStrip(
                    week = state.week,
                    selectedDay = selectedDay,
                    goal = state.dailyGoal,
                    onSelect = onSelectDay,
                    onShiftWeek = { onShiftDay(it * 7L) },
                    onToday = onToday
                )
            }
            item { SummaryCard(state, onEditMacros = { editMacros = true }) }
            items(MealType.entries, key = { it.name }) { meal ->
                MealCard(
                    meal = meal,
                    entries = state.entries.filter { it.mealType == meal },
                    photos = photos.filter { it.mealType == meal },
                    goal = state.dailyGoal * meal.goalShare,
                    onAdd = { onAddFood(selectedDay, meal) },
                    onEntryClick = { sheetEntry = it },
                    onRequestPhoto = { camera -> onRequestPhoto(meal, camera) },
                    onPhotoClick = { viewedPhoto = it }
                )
            }
            item(key = "water") { WaterCard(waterMl, waterGoalMl, onSetWater) }
        }
    }

    sheetEntry?.let { entry ->
        EntryDetailsSheet(
            entry = entry,
            state = state,
            onDismiss = { sheetEntry = null },
            onDelete = { onDelete(entry.id); sheetEntry = null },
            onOpenFood = entry.foodId?.let { id -> { sheetEntry = null; onOpenFood(id, selectedDay, entry.mealType) } },
            onOpenRecipe = entry.recipeId?.let { id -> { sheetEntry = null; onOpenRecipe(id, selectedDay, entry.mealType) } }
        )
    }
    if (editMacros) {
        MacroGoalsDialog(
            state = state,
            onDismiss = { editMacros = false },
            onSave = { grams, alsoCalories -> onSetMacros(grams, alsoCalories); editMacros = false }
        )
    }
    viewedPhoto?.let { photo ->
        PhotoViewer(photo, onDismiss = { viewedPhoto = null }, onDelete = { onDeletePhoto(photo); viewedPhoto = null })
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

private fun greeting(): String = when (java.time.LocalTime.now().hour) {
    in 5..11 -> "Доброе утро"
    in 12..17 -> "Добрый день"
    in 18..22 -> "Добрый вечер"
    else -> "Доброй ночи"
}

@Composable
private fun StreakChip(days: Int) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.padding(end = 12.dp)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocalFireDepartment, null, tint = Color(0xFFEB6834), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("$days", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

private val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru"))
private val weekdayShort = DateTimeFormatter.ofPattern("EEE", Locale("ru"))

/**
 * Monday-to-Sunday strip; each day shows a mini ring of calories against the
 * goal, the selected day is filled, today has a dot. Arrows move a week.
 */
@Composable
private fun WeekStrip(
    week: List<com.example.calorietracker.data.DayTotals>,
    selectedDay: Long,
    goal: Double,
    onSelect: (Long) -> Unit,
    onShiftWeek: (Int) -> Unit,
    onToday: () -> Unit
) {
    val today = LocalDate.now().toEpochDay()
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(LocalDate.ofEpochDay(selectedDay).format(monthFormatter).replaceFirstChar { it.uppercase() }, label = "month") {
                Text(it, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp))
            }
            Spacer(Modifier.weight(1f))
            if (selectedDay != today) TextButton(onClick = onToday) { Text("Сегодня") }
            IconButton(onClick = { onShiftWeek(-1) }) { Icon(Icons.Filled.ChevronLeft, "Предыдущая неделя") }
            IconButton(onClick = { onShiftWeek(1) }) { Icon(Icons.Filled.ChevronRight, "Следующая неделя") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            week.forEach { d ->
                DayCell(
                    date = LocalDate.ofEpochDay(d.epochDay),
                    progress = if (goal > 0) (d.calories / goal).toFloat() else 0f,
                    selected = d.epochDay == selectedDay,
                    isToday = d.epochDay == today,
                    onClick = { onSelect(d.epochDay) }
                )
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, progress: Float, selected: Boolean, isToday: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "daybg")
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val ring = if (progress > 1.05f) com.example.calorietracker.ui.theme.StatusCritical else if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    val track = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
    val sweep by animateFloatAsState(progress.coerceIn(0f, 1f), label = "dayring")
    Column(
        Modifier.clip(RoundedCornerShape(18.dp)).background(bg).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            date.format(weekdayShort).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) fg else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 3.dp.toPx()
                val d = size.minDimension - stroke
                val tl = Offset(stroke / 2, stroke / 2)
                drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(stroke))
                if (sweep > 0f) drawArc(ring, -90f, 360f * sweep, false, tl, Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Text("${date.dayOfMonth}", style = MaterialTheme.typography.labelLarge, color = fg)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.size(4.dp).clip(CircleShape).background(if (isToday) (if (selected) fg else MaterialTheme.colorScheme.primary) else Color.Transparent))
    }
}

@Composable
private fun SummaryCard(state: DiaryUiState, onEditMacros: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        val glow = Brush.verticalGradient(
            listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), MaterialTheme.colorScheme.surfaceContainer)
        )
        Column(Modifier.background(glow).padding(horizontal = 16.dp, vertical = 20.dp)) {
            CalorieSummary(
                caloriesByMeal = MealType.entries.associateWith { state.caloriesFor(it) },
                goal = state.dailyGoal,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (state.customMacros != null) "БЖУ · мои нормы" else "БЖУ",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onEditMacros) {
                    Icon(Icons.Filled.Edit, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Изменить")
                }
            }
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
    photos: List<MealPhoto>,
    goal: Double,
    onAdd: () -> Unit,
    onEntryClick: (DiaryEntry) -> Unit,
    onRequestPhoto: (fromCamera: Boolean) -> Unit,
    onPhotoClick: (MealPhoto) -> Unit
) {
    val color = mealColor(meal)
    val eaten = entries.sumOf { it.calories }
    var photoMenu by remember { mutableStateOf(false) }
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
            IconBadge(meal.icon, color)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(meal.label(), style = MaterialTheme.typography.titleMedium)
                Text(
                    "${eaten.roundToInt()} / ${goal.roundToInt()} ккал",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                val progress by animateFloatAsState(if (goal > 0) (eaten / goal).toFloat() else 0f, label = "meal-progress")
                ThinBar(progress, color, Modifier.width(120.dp), height = 4.dp)
            }
            Box {
                IconButton(onClick = { photoMenu = true }) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = "Прикрепить фото", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = photoMenu, onDismissRequest = { photoMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Сделать фото") },
                        leadingIcon = { Icon(Icons.Outlined.PhotoCamera, null) },
                        onClick = { photoMenu = false; onRequestPhoto(true) }
                    )
                    DropdownMenuItem(
                        text = { Text("Выбрать из галереи") },
                        leadingIcon = { Icon(Icons.Outlined.PhotoLibrary, null) },
                        onClick = { photoMenu = false; onRequestPhoto(false) }
                    )
                }
            }
            FilledTonalIconButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить в «${meal.label()}»")
            }
        }
        if (photos.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(photos, key = { it.id }) { photo ->
                    AsyncImage(
                        model = File(photo.path),
                        contentDescription = "Фото приёма пищи",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(72.dp).clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onPhotoClick(photo) }
                    )
                }
            }
        }
        if (entries.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            entries.forEach { entry -> EntryRow(entry) { onEntryClick(entry) } }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun EntryRow(entry: DiaryEntry, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
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
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val WaterBlueLight = Color(0xFF2A78D6)
private val WaterBlueDark = Color(0xFF3987E5)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WaterCard(ml: Int, goalMl: Int, onSet: (Int) -> Unit) {
    val glass = 250
    val cups = ((goalMl + glass - 1) / glass).coerceIn(4, 14)
    val filled = ml / glass
    val water = if (isDarkSurface()) WaterBlueDark else WaterBlueLight
    SectionCard(
        title = "Вода",
        subtitle = "Стакан — 250 мл. Норма рассчитана по весу и активности",
        action = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedNumber(ml, MaterialTheme.typography.titleLarge)
                Text("из $goalMl мл", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(cups) { i ->
                val isFilled = i < filled
                val scale by animateFloatAsState(if (isFilled) 1f else 0.85f, spring(dampingRatio = 0.4f), label = "cup")
                val tint by animateColorAsState(if (isFilled) water else MaterialTheme.colorScheme.outlineVariant, label = "cupColor")
                Box(
                    Modifier.size(36.dp).clip(CircleShape)
                        .clickable { onSet(if (filled == i + 1) i * glass else (i + 1) * glass) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isFilled) Icons.Filled.WaterDrop else Icons.Outlined.WaterDrop,
                        contentDescription = "${(i + 1) * glass} мл",
                        tint = tint,
                        modifier = Modifier.size(28.dp).scale(scale)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        ThinBar(if (goalMl > 0) ml.toFloat() / goalMl else 0f, water)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryDetailsSheet(
    entry: DiaryEntry,
    state: DiaryUiState,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onOpenFood: (() -> Unit)?,
    onOpenRecipe: (() -> Unit)?
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(entry.foodName, style = MaterialTheme.typography.headlineSmall)
            Text(
                entry.mealType.label() + if (entry.grams > 0) " · ${entry.grams.roundToInt()} г" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${entry.calories.roundToInt()}", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.width(6.dp))
                Text(
                    "ккал · ${(entry.calories / state.dailyGoal * 100).roundToInt()}% дневной нормы",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            val goals = state.macroGoals
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MacroBar("Углеводы", entry.carbs, goals.carbs, macroColor(Macro.CARBS), Modifier.weight(1f))
                MacroBar("Белки", entry.protein, goals.protein, macroColor(Macro.PROTEIN), Modifier.weight(1f))
                MacroBar("Жиры", entry.fat, goals.fat, macroColor(Macro.FAT), Modifier.weight(1f))
            }
            Text(
                "Полоски — доля от дневной нормы БЖУ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(Modifier.height(20.dp))
            if (onOpenFood != null) {
                OutlinedButton(onClick = onOpenFood, modifier = Modifier.fillMaxWidth()) { Text("Открыть карточку продукта") }
            }
            if (onOpenRecipe != null) {
                OutlinedButton(onClick = onOpenRecipe, modifier = Modifier.fillMaxWidth()) { Text("Открыть рецепт") }
            }
            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("Удалить из дневника", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun PhotoViewer(photo: MealPhoto, onDismiss: () -> Unit, onDelete: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(
                model = File(photo.path),
                contentDescription = "Фото приёма пищи",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            Row(Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Удалить фото", tint = Color.White) }
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Закрыть", tint = Color.White) }
            }
        }
    }
}

/**
 * Returns a callback that takes a photo with the camera or picks one from the
 * gallery and hands the resulting Uri to [onPhoto]. The camera needs the
 * CAMERA permission because the app declares it for the barcode scanner.
 */
@Composable
private fun rememberMealPhotoPicker(onPhoto: (Uri, MealType) -> Unit): (MealType, Boolean) -> Unit {
    val context = LocalContext.current
    var pendingMeal by remember { mutableStateOf(MealType.BREAKFAST) }
    val cameraUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", PhotoStore.cameraFile(context))
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onPhoto(it, pendingMeal) }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) onPhoto(cameraUri, pendingMeal)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) camera.launch(cameraUri)
    }
    return { meal, fromCamera ->
        pendingMeal = meal
        if (!fromCamera) {
            gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            camera.launch(cameraUri)
        } else {
            permission.launch(Manifest.permission.CAMERA)
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

/** Lets the user type daily macro goals in grams; shows the calories they add up to. */
@Composable
private fun MacroGoalsDialog(state: DiaryUiState, onDismiss: () -> Unit, onSave: (MacroGrams?, Boolean) -> Unit) {
    val current = state.macroGoals
    var p by remember { mutableStateOf(current.protein.roundToInt().toString()) }
    var f by remember { mutableStateOf(current.fat.roundToInt().toString()) }
    var c by remember { mutableStateOf(current.carbs.roundToInt().toString()) }
    var alsoCalories by remember { mutableStateOf(true) }
    val grams = listOf(p, f, c).map { it.replace(',', '.').toDoubleOrNull() }
    val valid = grams.all { it != null && it >= 0 }
    val kcal = if (valid) MacroGrams(grams[0]!!, grams[1]!!, grams[2]!!).calories else 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Мои нормы БЖУ") },
        text = {
            Column {
                Text("Граммы в день. Калории посчитаются сами: белки и углеводы по 4 ккал, жиры по 9.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                val kb = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                OutlinedTextField(p, { p = it }, label = { Text("Белки, г") }, singleLine = true, keyboardOptions = kb)
                OutlinedTextField(f, { f = it }, label = { Text("Жиры, г") }, singleLine = true, keyboardOptions = kb)
                OutlinedTextField(c, { c = it }, label = { Text("Углеводы, г") }, singleLine = true, keyboardOptions = kb)
                Spacer(Modifier.height(12.dp))
                Text("= ${kcal.roundToInt()} ккал", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = alsoCalories, onCheckedChange = { alsoCalories = it })
                    Text("Сделать это целью по калориям (сейчас ${state.dailyGoal.roundToInt()})", style = MaterialTheme.typography.bodyMedium)
                }
                if (state.customMacros != null) {
                    TextButton(onClick = { onSave(null, false) }) { Text("Сбросить — считать автоматически") }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid && kcal > 0, onClick = { onSave(MacroGrams(grams[0]!!, grams[1]!!, grams[2]!!), alsoCalories) }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
