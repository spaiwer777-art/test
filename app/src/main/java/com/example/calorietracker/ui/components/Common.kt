package com.example.calorietracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.calorietracker.ui.theme.Macro
import com.example.calorietracker.ui.theme.macroColor
import kotlin.math.roundToInt

/** Rounded card with an optional title row; the building block of every screen. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        if (subtitle != null) {
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    action?.invoke()
                }
                Spacer(Modifier.height(16.dp))
            }
            content()
        }
    }
}

/** Small colored circle with an icon, used for meal / category / tool markers. */
@Composable
fun IconBadge(icon: ImageVector, tint: Color, size: Dp = 44.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun Pill(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.14f)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Nutri-Score letter with its official color; the letter itself carries the meaning. */
@Composable
fun NutriScoreBadge(grade: String) {
    val g = grade.uppercase()
    val color = when (g) {
        "A" -> Color(0xFF038141)
        "B" -> Color(0xFF85BB2F)
        "C" -> Color(0xFFFECB02)
        "D" -> Color(0xFFEE8100)
        else -> Color(0xFFE63E11)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Nutri-Score", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Box(Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(color), contentAlignment = Alignment.Center) {
            Text(g, style = MaterialTheme.typography.labelLarge, color = if (g == "C") Color.Black else Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Donut of where the energy comes from (protein / fat / carbs, kcal-weighted),
 * with a legend that states grams and share so color is never the only cue.
 */
@Composable
fun MacroDonut(protein: Double, fat: Double, carbs: Double, modifier: Modifier = Modifier, unit: String = "г") {
    val parts = listOf(
        Triple("Углеводы", carbs, Macro.CARBS),
        Triple("Белки", protein, Macro.PROTEIN),
        Triple("Жиры", fat, Macro.FAT)
    )
    val energy = parts.map { (_, g, m) -> g * if (m == Macro.FAT) 9 else 4 }
    val total = energy.sum()
    val colors = parts.map { macroColor(it.third) }
    val track = MaterialTheme.colorScheme.surfaceVariant
    val inspection = LocalInspectionMode.current
    val sweep = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(Unit) { sweep.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(96.dp)) {
            val stroke = 14.dp.toPx()
            val d = size.minDimension - stroke
            val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
            drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(stroke))
            if (total > 0) {
                val gap = Math.toDegrees((2.dp.toPx() / (d / 2)).toDouble()).toFloat()
                var start = -90f
                energy.forEachIndexed { i, e ->
                    val s = (360f * (e / total).toFloat()) * sweep.value
                    if (s > gap) drawArc(colors[i], start + gap / 2, s - gap, false, tl, Size(d, d), style = Stroke(stroke))
                    start += s
                }
            }
        }
        Spacer(Modifier.width(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            parts.forEachIndexed { i, (label, grams, _) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(colors[i]))
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(84.dp))
                    Text(
                        "${formatGrams(grams)} $unit",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.width(64.dp)
                    )
                    val share = if (total > 0) (energy[i] / total * 100).roundToInt() else 0
                    Text("$share%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** 12.3 -> "12,3", 12.0 -> "12"; grams shown to one decimal only when it matters. */
fun formatGrams(value: Double): String =
    if (value >= 10 || value % 1.0 == 0.0) value.roundToInt().toString()
    else String.format(java.util.Locale("ru"), "%.1f", value)

/** Always one decimal, e.g. body weight "80,8"; [floor] avoids 24.98 showing as "25,0". */
fun formatOneDecimal(value: Double, floor: Boolean = false): String {
    val v = if (floor) kotlin.math.floor(value * 10) / 10 else value
    return String.format(java.util.Locale("ru"), "%.1f", v)
}

/** Parses user input that may use a comma as decimal separator. */
fun String.toNumberOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

@Composable
fun EmptyState(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Thin progress bar with rounded ends. */
@Composable
fun ThinBar(progress: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    Box(
        modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxSize()
                .clip(RoundedCornerShape(height / 2)).background(color)
        )
    }
}

/** Warning banner for a food or recipe that conflicts with the active diet; icon + text, not color alone. */
@Composable
fun DietWarning(text: String) {
    Surface(shape = MaterialTheme.shapes.medium, color = Color(0xFFFAB219).copy(alpha = 0.18f), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(androidx.compose.material.icons.Icons.Filled.Warning, null, tint = Color(0xFFB57F00))
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
