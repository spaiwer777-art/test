package com.example.calorietracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.calorietracker.data.MealType
import com.example.calorietracker.ui.theme.mealColor
import java.time.LocalTime

fun MealType.label(): String = when (this) {
    MealType.BREAKFAST -> "Завтрак"
    MealType.LUNCH -> "Обед"
    MealType.DINNER -> "Ужин"
    MealType.SNACK -> "Перекус"
}

val MealType.icon: ImageVector
    get() = when (this) {
        MealType.BREAKFAST -> Icons.Filled.FreeBreakfast
        MealType.LUNCH -> Icons.Filled.LunchDining
        MealType.DINNER -> Icons.Filled.DinnerDining
        MealType.SNACK -> Icons.Filled.Cookie
    }

/** Best guess for which meal the user is logging right now. */
fun mealForCurrentTime(): MealType {
    val hour = LocalTime.now().hour
    return when {
        hour < 11 -> MealType.BREAKFAST
        hour < 16 -> MealType.LUNCH
        hour < 21 -> MealType.DINNER
        else -> MealType.SNACK
    }
}

@Composable
fun MealSelector(selected: MealType, onSelect: (MealType) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MealType.entries.forEach { meal ->
            val color = mealColor(meal)
            FilterChip(
                selected = meal == selected,
                onClick = { onSelect(meal) },
                label = { Text(meal.label()) },
                leadingIcon = {
                    Icon(meal.icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.18f)
                )
            )
        }
    }
}
