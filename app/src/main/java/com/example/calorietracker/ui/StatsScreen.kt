package com.example.calorietracker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.data.DayTotals
import com.example.calorietracker.ui.components.AnimatedNumber
import com.example.calorietracker.ui.theme.Macro
import com.example.calorietracker.ui.theme.macroColor
import com.example.calorietracker.viewmodel.StatsRange
import com.example.calorietracker.viewmodel.StatsUiState
import com.example.calorietracker.viewmodel.StatsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun StatsScreen(bottomBar: @Composable () -> Unit) {
    val vm: StatsViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    StatsContent(state, onRange = vm::setRange, bottomBar = bottomBar)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatsContent(state: StatsUiState, onRange: (StatsRange) -> Unit, bottomBar: @Composable () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Статистика", style = MaterialTheme.typography.headlineSmall) }) },
        bottomBar = bottomBar
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
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

            StatCard {
                Text("Калории по дням", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Нажми на столбец, чтобы увидеть значение",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                if (state.loggedDayCount == 0) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Пока нет записей за этот период",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    CaloriesBarChart(state.days, state.dailyGoal, state.range)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Среднее в день", state.averageCalories.roundToInt(), "ккал", Modifier.weight(1f))
                StatTile("Дней в норме", state.daysWithinGoal, "из ${state.loggedDayCount}", Modifier.weight(1f))
                StatTile("Белок в день", state.averageProtein.roundToInt(), "г", Modifier.weight(1f))
            }

            StatCard {
                Text("Баланс БЖУ", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Доля калорий из белков, жиров и углеводов",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                MacroSplitBar(state)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(20.dp)) { content() }
    }
}

@Composable
private fun StatTile(label: String, value: Int, unit: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            AnimatedNumber(value, MaterialTheme.typography.headlineSmall)
            Text(unit, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val weekdayFormatter = DateTimeFormatter.ofPattern("EEE", Locale("ru"))
private val tooltipDateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale("ru"))

/**
 * Daily calories as rounded-top bars on one axis, with a dashed goal line.
 * Bars grow in when the range changes; tapping a column shows its value.
 */
@Composable
private fun CaloriesBarChart(days: List<DayTotals>, goal: Double, range: StatsRange) {
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val mutedText = MaterialTheme.colorScheme.onSurfaceVariant
    val tooltipBg = MaterialTheme.colorScheme.inverseSurface
    val tooltipText = MaterialTheme.colorScheme.inverseOnSurface
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
        val barW = (slot * if (range == StatsRange.WEEK) 0.55f else 0.7f).coerceAtLeast(2.dp.toPx())
        fun y(v: Float) = chartH - v / maxValue * chartH

        // Recessive baseline and goal line.
        drawLine(gridColor, Offset(0f, chartH), Offset(size.width, chartH), 1.dp.toPx())
        val goalY = y(goal.toFloat())
        drawLine(
            mutedText, Offset(0f, goalY), Offset(size.width, goalY), 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
        )
        val goalLabel = measurer.measure("цель ${goal.roundToInt()}", labelStyle.copy(color = mutedText))
        drawText(goalLabel, topLeft = Offset(size.width - goalLabel.size.width, goalY - goalLabel.size.height - 2.dp.toPx()))

        val radius = 4.dp.toPx()
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
            val date = LocalDate.ofEpochDay(d.epochDay)
            val showLabel = range == StatsRange.WEEK || i % 5 == 0 || i == days.lastIndex
            if (showLabel) {
                val text = if (range == StatsRange.WEEK) date.format(weekdayFormatter).replaceFirstChar { it.uppercase() } else date.dayOfMonth.toString()
                val layout = measurer.measure(text, labelStyle.copy(color = mutedText))
                drawText(layout, topLeft = Offset(cx - layout.size.width / 2, chartH + 6.dp.toPx()))
            }
        }

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
