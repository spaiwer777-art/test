package com.example.calorietracker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.example.calorietracker.ui.components.AppTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.data.DayTotals
import com.example.calorietracker.data.MealType
import com.example.calorietracker.data.WeightEntry
import com.example.calorietracker.ui.components.AnimatedNumber
import com.example.calorietracker.ui.components.EmptyState
import com.example.calorietracker.ui.components.MacroBar
import com.example.calorietracker.ui.components.SectionCard
import com.example.calorietracker.ui.components.ThinBar
import com.example.calorietracker.ui.components.formatGrams
import com.example.calorietracker.ui.components.formatOneDecimal
import com.example.calorietracker.ui.components.icon
import com.example.calorietracker.ui.components.label
import com.example.calorietracker.ui.components.toNumberOrNull
import com.example.calorietracker.ui.theme.Macro
import com.example.calorietracker.ui.theme.macroColor
import com.example.calorietracker.ui.theme.mealColor
import com.example.calorietracker.viewmodel.MacroGoals
import com.example.calorietracker.viewmodel.StatsRange
import com.example.calorietracker.viewmodel.StatsUiState
import com.example.calorietracker.viewmodel.StatsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun StatsScreen(bottomBar: @Composable () -> Unit) {
    val vm: StatsViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    StatsContent(state, onRange = vm::setRange, onLogWeight = vm::logWeight, bottomBar = bottomBar)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatsContent(
    state: StatsUiState,
    onRange: (StatsRange) -> Unit,
    onLogWeight: (Double) -> Unit,
    bottomBar: @Composable () -> Unit
) {
    var weightDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Статистика", style = MaterialTheme.typography.headlineSmall) }) },
        bottomBar = bottomBar
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                StatsRange.entries.forEachIndexed { i, r ->
                    SegmentedButton(
                        selected = state.range == r,
                        onClick = { onRange(r) },
                        shape = SegmentedButtonDefaults.itemShape(i, StatsRange.entries.size)
                    ) { Text(r.label) }
                }
            }

            CaloriesCard(state)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Дней в норме", "${state.daysWithinGoal}", "из ${state.loggedDayCount} с записями", Modifier.weight(1f))
                StatTile("Серия", "${state.streak}", daysWord(state.streak) + " подряд", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Белок в день", "${state.averageProtein.roundToInt()}", "г в среднем", Modifier.weight(1f))
                StatTile("Вода в день", "${state.averageWater.roundToInt()}", "мл из ${state.waterGoal}", Modifier.weight(1f))
            }

            WeightCard(state.weights, state.range, onAdd = { weightDialog = true })
            MealsCard(state)
            MacrosCard(state)
            TopFoodsCard(state)
            Spacer(Modifier.height(16.dp))
        }
    }
    if (weightDialog) {
        WeightDialog(
            initial = state.weights.lastOrNull()?.kg,
            onDismiss = { weightDialog = false },
            onSave = { onLogWeight(it); weightDialog = false }
        )
    }
}

private fun daysWord(n: Int): String = when {
    n % 100 in 11..14 -> "дней"
    n % 10 == 1 -> "день"
    n % 10 in 2..4 -> "дня"
    else -> "дней"
}

@Composable
private fun CaloriesCard(state: StatsUiState) {
    SectionCard(title = "Калории", subtitle = "Среднее по дням с записями") {
        Row(verticalAlignment = Alignment.Bottom) {
            AnimatedNumber(state.averageCalories.roundToInt(), MaterialTheme.typography.displaySmall)
            Spacer(Modifier.width(6.dp))
            Text("ккал/день", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
        }
        if (state.loggedDayCount > 0) {
            val diffGoal = state.averageCalories - state.dailyGoal
            Trend(
                icon = if (diffGoal > 0) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                text = if (abs(diffGoal) < 10) "ровно по цели ${state.dailyGoal.roundToInt()} ккал"
                else "на ${abs(diffGoal).roundToInt()} ккал ${if (diffGoal > 0) "выше" else "ниже"} цели ${state.dailyGoal.roundToInt()}"
            )
            state.previousAverage?.let { prev ->
                val d = state.averageCalories - prev
                Trend(
                    icon = when {
                        abs(d) < 10 -> Icons.Filled.TrendingFlat
                        d > 0 -> Icons.Filled.TrendingUp
                        else -> Icons.Filled.TrendingDown
                    },
                    text = if (abs(d) < 10) "как в прошлые ${state.range.days} дней"
                    else "на ${abs(d).roundToInt()} ккал ${if (d > 0) "больше" else "меньше"}, чем в прошлые ${state.range.days} дней"
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        if (state.loggedDayCount == 0) {
            EmptyState(Icons.Filled.BarChart, "Пока нет записей за этот период")
        } else {
            CaloriesBarChart(state.days, state.dailyGoal, state.range)
            Spacer(Modifier.height(10.dp))
            ChartLegend(showAverage = state.range != StatsRange.WEEK)
        }
    }
}

@Composable
private fun Trend(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChartLegend(showAverage: Boolean) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem("за день") { Box(Modifier.size(10.dp).clip(MaterialTheme.shapes.extraSmall).background(MaterialTheme.colorScheme.primary)) }
        if (showAverage) {
            val ink = MaterialTheme.colorScheme.onSurface
            LegendItem("среднее за 7 дней") { Canvas(Modifier.width(16.dp).height(10.dp)) { drawLine(ink, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 2.dp.toPx()) } }
        }
        LegendItem("цель") {
            Canvas(Modifier.width(16.dp).height(10.dp)) {
                drawLine(muted, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
            }
        }
    }
}

@Composable
private fun LegendItem(text: String, mark: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        mark()
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatTile(label: String, value: String, unit: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(unit, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private val weekdayFormatter = DateTimeFormatter.ofPattern("EEE", Locale("ru"))
private val shortDateFormatter = DateTimeFormatter.ofPattern("d.MM", Locale("ru"))
private val tooltipDateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale("ru"))

/**
 * Daily calories as rounded-top bars on one axis, a dashed goal line and, for
 * 30/90 days, a 7-day moving average line (same unit, same axis). Bars grow in
 * when the range changes; tapping a column shows its value.
 */
@Composable
private fun CaloriesBarChart(days: List<DayTotals>, goal: Double, range: StatsRange) {
    val barColor = MaterialTheme.colorScheme.primary
    val avgColor = MaterialTheme.colorScheme.onSurface
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val mutedText = MaterialTheme.colorScheme.onSurfaceVariant
    val tooltipBg = MaterialTheme.colorScheme.inverseSurface
    val tooltipText = MaterialTheme.colorScheme.inverseOnSurface
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val labelStyle = MaterialTheme.typography.labelSmall
    val measurer = rememberTextMeasurer()

    val inspection = LocalInspectionMode.current
    val grow = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(range) {
        if (inspection) return@LaunchedEffect
        grow.snapTo(0f)
        grow.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
    }
    var selected by remember(range) { mutableStateOf<Int?>(null) }

    // Trailing 7-day average over logged days only, so empty days don't drag it down.
    val movingAvg = remember(days) {
        days.indices.map { i ->
            val window = days.subList(maxOf(0, i - 6), i + 1).filter { it.calories > 0 }
            if (window.isEmpty()) null else window.sumOf { it.calories } / window.size
        }
    }
    val maxValue = maxOf(goal * 1.15, (days.maxOfOrNull { it.calories } ?: 0.0) * 1.1).toFloat()

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(220.dp)
            .pointerInput(days) {
                detectTapGestures { pos ->
                    val slot = size.width / days.size
                    val i = (pos.x / slot).toInt().coerceIn(0, days.lastIndex)
                    selected = if (selected == i || days[i].calories <= 0) null else i
                }
            }
    ) {
        val bottomPad = 22.dp.toPx()
        val chartH = size.height - bottomPad
        val slot = size.width / days.size
        val barW = (slot * when (range) {
            StatsRange.WEEK -> 0.55f
            StatsRange.MONTH -> 0.7f
            StatsRange.QUARTER -> 0.8f
        }).coerceAtLeast(1.5.dp.toPx())
        fun y(v: Float) = chartH - v / maxValue * chartH

        drawLine(gridColor, Offset(0f, chartH), Offset(size.width, chartH), 1.dp.toPx())
        val goalY = y(goal.toFloat())
        drawLine(
            mutedText, Offset(0f, goalY), Offset(size.width, goalY), 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
        )
        val radius = if (range == StatsRange.QUARTER) 1.5.dp.toPx() else 4.dp.toPx()
        val labelEvery = when (range) {
            StatsRange.WEEK -> 1
            StatsRange.MONTH -> 5
            StatsRange.QUARTER -> 15
        }
        days.forEachIndexed { i, d ->
            val cx = slot * i + slot / 2
            if (d.calories > 0) {
                val top = y(d.calories.toFloat() * grow.value)
                val alpha = if (selected == null || selected == i) 1f else 0.45f
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = cx - barW / 2, top = top, right = cx + barW / 2, bottom = chartH,
                            topLeftCornerRadius = CornerRadius(radius), topRightCornerRadius = CornerRadius(radius),
                            bottomLeftCornerRadius = CornerRadius.Zero, bottomRightCornerRadius = CornerRadius.Zero
                        )
                    )
                }
                drawPath(path, barColor.copy(alpha = alpha))
            }
            if (i % labelEvery == 0 || i == days.lastIndex) {
                val date = LocalDate.ofEpochDay(d.epochDay)
                val text = when (range) {
                    StatsRange.WEEK -> date.format(weekdayFormatter).replaceFirstChar { it.uppercase() }
                    StatsRange.MONTH -> date.dayOfMonth.toString()
                    StatsRange.QUARTER -> date.format(shortDateFormatter)
                }
                val layout = measurer.measure(text, labelStyle.copy(color = mutedText))
                val x = (cx - layout.size.width / 2).coerceIn(0f, size.width - layout.size.width)
                drawText(layout, topLeft = Offset(x, chartH + 6.dp.toPx()))
            }
        }

        if (range != StatsRange.WEEK) {
            val line = Path()
            var started = false
            movingAvg.forEachIndexed { i, v ->
                if (v == null) return@forEachIndexed
                val p = Offset(slot * i + slot / 2, y(v.toFloat() * grow.value))
                if (!started) { line.moveTo(p.x, p.y); started = true } else line.lineTo(p.x, p.y)
            }
            // Surface halo keeps the line readable where it crosses bars.
            drawPath(line, surface, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
            drawPath(line, avgColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }

        // Goal label on a surface-colored chip so bars behind it don't make it unreadable.
        val goalLabel = measurer.measure("цель ${goal.roundToInt()}", labelStyle.copy(color = mutedText))
        val chipPad = 4.dp.toPx()
        val chipLeft = size.width - goalLabel.size.width - chipPad * 2
        val chipTop = goalY - goalLabel.size.height / 2 - chipPad
        drawRoundRect(
            surface, Offset(chipLeft, chipTop),
            Size(goalLabel.size.width + chipPad * 2, goalLabel.size.height + chipPad * 2), CornerRadius(6.dp.toPx())
        )
        drawText(goalLabel, topLeft = Offset(chipLeft + chipPad, chipTop + chipPad))

        selected?.let { i ->
            val d = days[i]
            val cx = slot * i + slot / 2
            val text = "${d.calories.roundToInt()} ккал\n${LocalDate.ofEpochDay(d.epochDay).format(tooltipDateFormatter)}"
            val layout = measurer.measure(text, TextStyle(color = tooltipText, fontSize = labelStyle.fontSize))
            val padH = 8.dp.toPx()
            val padV = 6.dp.toPx()
            val w = layout.size.width + padH * 2
            val h = layout.size.height + padV * 2
            val left = (cx - w / 2).coerceIn(0f, size.width - w)
            val top = (y(d.calories.toFloat()) - h - 6.dp.toPx()).coerceAtLeast(0f)
            drawRoundRect(tooltipBg, Offset(left, top), Size(w, h), CornerRadius(8.dp.toPx()))
            drawText(layout, topLeft = Offset(left + padH, top + padV))
        }
    }
}

@Composable
private fun WeightCard(weights: List<WeightEntry>, range: StatsRange, onAdd: () -> Unit) {
    SectionCard(
        title = "Вес",
        subtitle = "Записывай утром натощак — так динамика честнее",
        action = {
            FilledTonalButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Записать")
            }
        }
    ) {
        if (weights.isEmpty()) {
            EmptyState(Icons.Filled.MonitorWeight, "Нет записей веса за этот период")
            return@SectionCard
        }
        val last = weights.last().kg
        val change = last - weights.first().kg
        Row(verticalAlignment = Alignment.Bottom) {
            Text(formatOneDecimal(last), style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.width(6.dp))
            Text("кг", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
            Spacer(Modifier.width(16.dp))
            if (weights.size > 1) {
                Trend(
                    icon = when {
                        abs(change) < 0.05 -> Icons.Filled.TrendingFlat
                        change > 0 -> Icons.Filled.TrendingUp
                        else -> Icons.Filled.TrendingDown
                    },
                    text = if (abs(change) < 0.05) "без изменений" else "${if (change > 0) "+" else "−"}${formatOneDecimal(abs(change))} кг за период"
                )
            }
        }
        if (weights.size > 1) {
            Spacer(Modifier.height(12.dp))
            WeightLine(weights, range)
        }
    }
}

/** Weight over the range as a 2dp line with point markers; x is calendar position. */
@Composable
private fun WeightLine(weights: List<WeightEntry>, range: StatsRange) {
    val color = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val grid = MaterialTheme.colorScheme.outlineVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall
    val measurer = rememberTextMeasurer()
    val today = LocalDate.now().toEpochDay()
    val from = today - range.days + 1
    val minKg = weights.minOf { it.kg } - 0.5
    val maxKg = weights.maxOf { it.kg } + 0.5
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        val left = 36.dp.toPx()
        val w = size.width - left - 8.dp.toPx()
        val h = size.height - 8.dp.toPx()
        fun x(day: Long) = left + (day - from).toFloat() / (range.days - 1).coerceAtLeast(1) * w
        fun y(kg: Double) = 4.dp.toPx() + ((maxKg - kg) / (maxKg - minKg)).toFloat() * h
        listOf(maxKg - 0.5, minKg + 0.5).forEach { kg ->
            drawLine(grid, Offset(left, y(kg)), Offset(size.width, y(kg)), 1.dp.toPx())
            val t = measurer.measure(formatOneDecimal(kg), labelStyle.copy(color = muted))
            drawText(t, topLeft = Offset(0f, y(kg) - t.size.height / 2))
        }
        val path = Path()
        weights.forEachIndexed { i, e -> if (i == 0) path.moveTo(x(e.epochDay), y(e.kg)) else path.lineTo(x(e.epochDay), y(e.kg)) }
        drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        weights.forEach { e ->
            drawCircle(surface, 6.dp.toPx(), Offset(x(e.epochDay), y(e.kg)))
            drawCircle(color, 4.dp.toPx(), Offset(x(e.epochDay), y(e.kg)))
        }
    }
}

@Composable
private fun MealsCard(state: StatsUiState) {
    val total = MealType.entries.sumOf { state.mealCalories(it) }
    SectionCard(title = "Приёмы пищи", subtitle = "Как калории распределяются за день") {
        if (total <= 0) {
            EmptyState(Icons.Filled.LocalFireDepartment, "Нет данных")
            return@SectionCard
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val colors = MealType.entries.map { mealColor(it) }
            val track = MaterialTheme.colorScheme.surfaceVariant
            val sweep by animateFloatAsState(1f, label = "meals")
            Canvas(Modifier.size(110.dp)) {
                val stroke = 16.dp.toPx()
                val d = size.minDimension - stroke
                val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
                drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(stroke))
                val gap = Math.toDegrees((2.dp.toPx() / (d / 2)).toDouble()).toFloat()
                var start = -90f
                MealType.entries.forEachIndexed { i, m ->
                    val s = 360f * (state.mealCalories(m) / total).toFloat() * sweep
                    if (s > gap) drawArc(colors[i], start + gap / 2, s - gap, false, tl, Size(d, d), style = Stroke(stroke))
                    start += s
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val days = state.loggedDayCount.coerceAtLeast(1)
                MealType.entries.forEach { m ->
                    val kcal = state.mealCalories(m)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(m.icon, null, Modifier.size(16.dp), tint = mealColor(m))
                        Spacer(Modifier.width(8.dp))
                        Text(m.label(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(76.dp))
                        Text("${(kcal / total * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(44.dp))
                        Text("${(kcal / days).roundToInt()} ккал", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun MacrosCard(state: StatsUiState) {
    val goals = MacroGoals.fromCalories(state.dailyGoal, state.split, state.customMacros)
    SectionCard(title = "БЖУ", subtitle = "Среднее в день против нормы и доля калорий") {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MacroBar("Углеводы", state.averageCarbs, goals.carbs, macroColor(Macro.CARBS), Modifier.weight(1f))
            MacroBar("Белки", state.averageProtein, goals.protein, macroColor(Macro.PROTEIN), Modifier.weight(1f))
            MacroBar("Жиры", state.averageFat, goals.fat, macroColor(Macro.FAT), Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        MacroSplitBar(state)
    }
}

@Composable
private fun MacroSplitBar(state: StatsUiState) {
    val (p, f, c) = state.macroEnergySplit
    val parts = listOf(
        Triple("Углеводы", c, macroColor(Macro.CARBS)),
        Triple("Белки", p, macroColor(Macro.PROTEIN)),
        Triple("Жиры", f, macroColor(Macro.FAT))
    )
    val inspection = LocalInspectionMode.current
    val grow = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(state.range) {
        if (inspection) return@LaunchedEffect
        grow.snapTo(0f)
        grow.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    val track = MaterialTheme.colorScheme.surfaceVariant

    Canvas(Modifier.fillMaxWidth().height(14.dp).clip(MaterialTheme.shapes.small)) {
        drawRect(track)
        val gap = 2.dp.toPx()
        var x = 0f
        parts.forEach { (_, share, color) ->
            val w = size.width * share.toFloat() * grow.value
            if (w > gap) drawRect(color, Offset(x, 0f), Size(w - gap, size.height))
            x += w
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        parts.forEach { (label, share, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(6.dp))
                Text("$label ${(share * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun TopFoodsCard(state: StatsUiState) {
    if (state.topFoods.isEmpty()) return
    val max = state.topFoods.maxOf { it.calories }
    SectionCard(title = "Больше всего калорий дали", subtitle = "Топ-5 за период") {
        state.topFoods.forEachIndexed { i, f ->
            Column(Modifier.padding(vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(20.dp))
                    Text(f.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${f.calories.roundToInt()} ккал", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(4.dp))
                Row {
                    Spacer(Modifier.width(20.dp))
                    Column(Modifier.weight(1f)) {
                        ThinBar((f.calories / max).toFloat(), MaterialTheme.colorScheme.primary, height = 4.dp)
                        Text("${f.times} раз", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightDialog(initial: Double?, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var text by remember { mutableStateOf(initial?.let { formatOneDecimal(it) } ?: "") }
    val value = text.toNumberOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Вес сегодня") },
        text = {
            AppTextField(
                value = text,
                onValueChange = { text = it },
                suffix = { Text("кг") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = {
            TextButton(onClick = { value?.let(onSave) }, enabled = value != null && value in 30.0..300.0) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
