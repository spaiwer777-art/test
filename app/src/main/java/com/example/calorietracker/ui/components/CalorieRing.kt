package com.example.calorietracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.calorietracker.data.MealType
import com.example.calorietracker.ui.theme.StatusCritical
import com.example.calorietracker.ui.theme.mealColor
import kotlin.math.roundToInt

/** Number that counts up/down to its new value instead of jumping. */
@Composable
fun AnimatedNumber(value: Int, style: androidx.compose.ui.text.TextStyle, color: Color = Color.Unspecified) {
    val animated by animateIntAsState(value, tween(700, easing = FastOutSlowInEasing), label = "number")
    Text(animated.toString(), style = style, color = color)
}

/**
 * Donut showing the day's calories split by meal. Each meal is its own colored
 * arc (in breakfast -> snack order, separated by a small gap), the rest of the
 * ring is the unused part of the goal. When the goal is exceeded the arcs are
 * scaled to fill the full circle and the center switches to an over-goal state.
 */
@Composable
fun CalorieRing(
    caloriesByMeal: Map<MealType, Double>,
    goal: Double,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 18.dp
) {
    val eaten = caloriesByMeal.values.sum()
    val remaining = goal - eaten
    val over = remaining < 0
    val scale = maxOf(goal, eaten).takeIf { it > 0 } ?: 1.0

    // Sweep in from zero on first show, then animate each arc to new values.
    // Previews/screenshots render a single frame, so start them fully drawn.
    val inspection = LocalInspectionMode.current
    val intro = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(Unit) { intro.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }

    val fractions = MealType.entries.map { meal ->
        val target = ((caloriesByMeal[meal] ?: 0.0) / scale).toFloat()
        animateFloatAsState(target, tween(700, easing = FastOutSlowInEasing), label = "arc-$meal").value
    }
    val colors = MealType.entries.map { mealColor(it) }
    val track = MaterialTheme.colorScheme.surfaceVariant

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val diameter = this.size.minDimension - stroke
            val topLeft = Offset((this.size.width - diameter) / 2, (this.size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))

            // 2dp surface gap between neighbouring arcs, expressed in degrees.
            val gapDeg = Math.toDegrees((2.dp.toPx() / (diameter / 2)).toDouble()).toFloat()
            var start = -90f
            fractions.forEachIndexed { i, f ->
                val sweep = 360f * f * intro.value
                if (sweep > gapDeg) {
                    drawArc(
                        color = colors[i],
                        startAngle = start + gapDeg / 2,
                        sweepAngle = sweep - gapDeg,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Butt)
                    )
                }
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (over) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(20.dp))
            }
            AnimatedNumber(
                value = kotlin.math.abs(remaining).roundToInt(),
                style = MaterialTheme.typography.displaySmall,
                color = if (over) StatusCritical else MaterialTheme.colorScheme.onSurface
            )
            Text(
                if (over) "ккал сверх нормы" else "ккал осталось",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Eaten | ring | goal row used as the diary's hero. */
@Composable
fun CalorieSummary(
    caloriesByMeal: Map<MealType, Double>,
    goal: Double,
    modifier: Modifier = Modifier,
    onGoalClick: (() -> Unit)? = null
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        SideStat("Съедено", caloriesByMeal.values.sum().roundToInt(), Modifier.weight(1f))
        CalorieRing(
            caloriesByMeal, goal, size = 184.dp,
            modifier = if (onGoalClick != null) Modifier.clip(CircleShape).clickable(onClick = onGoalClick) else Modifier
        )
        SideStat("Цель", goal.roundToInt(), Modifier.weight(1f), onGoalClick)
    }
}

@Composable
private fun SideStat(label: String, value: Int, modifier: Modifier, onClick: (() -> Unit)? = null) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedNumber(value, MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (onClick != null) {
                Spacer(Modifier.width(3.dp))
                Icon(Icons.Filled.Edit, "Изменить", Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
