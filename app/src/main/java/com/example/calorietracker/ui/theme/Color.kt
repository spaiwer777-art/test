package com.example.calorietracker.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.MaterialTheme
import com.example.calorietracker.data.AccentColor
import com.example.calorietracker.data.MealType

/** Primary + container steps for one accent, picked separately for light and dark surfaces. */
data class AccentSwatch(
    val lightPrimary: Color,
    val lightContainer: Color,
    val lightOnContainer: Color,
    val darkPrimary: Color,
    val darkContainer: Color,
    val darkOnContainer: Color
)

val AccentColor.swatch: AccentSwatch
    get() = when (this) {
        AccentColor.GREEN -> AccentSwatch(
            Color(0xFF2E7D32), Color(0xFFC8E6C9), Color(0xFF0B2E0D),
            Color(0xFF81C995), Color(0xFF1E4D2B), Color(0xFFC8F0D0)
        )
        AccentColor.TEAL -> AccentSwatch(
            Color(0xFF00796B), Color(0xFFB2DFDB), Color(0xFF00201C),
            Color(0xFF6FD6C6), Color(0xFF00423A), Color(0xFFB5F2E8)
        )
        AccentColor.BLUE -> AccentSwatch(
            Color(0xFF1F63C4), Color(0xFFD6E3FF), Color(0xFF001B3E),
            Color(0xFF9EC2FF), Color(0xFF1B3F73), Color(0xFFD6E3FF)
        )
        AccentColor.VIOLET -> AccentSwatch(
            Color(0xFF6750A4), Color(0xFFE9DDFF), Color(0xFF22005D),
            Color(0xFFCFBCFF), Color(0xFF4A3880), Color(0xFFE9DDFF)
        )
        AccentColor.PINK -> AccentSwatch(
            Color(0xFFB8336A), Color(0xFFFFD9E2), Color(0xFF3E001D),
            Color(0xFFFFB0C8), Color(0xFF7A1F48), Color(0xFFFFD9E2)
        )
        AccentColor.ORANGE -> AccentSwatch(
            Color(0xFFC25100), Color(0xFFFFDBC8), Color(0xFF331200),
            Color(0xFFFFB68C), Color(0xFF7A3300), Color(0xFFFFDBC8)
        )
    }

/**
 * Data colors for charts. These stay fixed regardless of the accent so a meal
 * always keeps its color. Categorical steps come from a palette validated for
 * color-vision deficiency, with separate steps for light and dark surfaces;
 * meal order (breakfast -> snack) keeps only validated neighbours adjacent in the ring.
 */
private val MealColorsLight = mapOf(
    MealType.BREAKFAST to Color(0xFFEDA100),
    MealType.LUNCH to Color(0xFF1BAF7A),
    MealType.DINNER to Color(0xFFEB6834),
    MealType.SNACK to Color(0xFF2A78D6)
)
private val MealColorsDark = mapOf(
    MealType.BREAKFAST to Color(0xFFC98500),
    MealType.LUNCH to Color(0xFF199E70),
    MealType.DINNER to Color(0xFFD95926),
    MealType.SNACK to Color(0xFF3987E5)
)

enum class Macro { PROTEIN, FAT, CARBS }

private val MacroColorsLight = mapOf(
    Macro.PROTEIN to Color(0xFF4A3AA7),
    Macro.FAT to Color(0xFFE87BA4),
    Macro.CARBS to Color(0xFF008300)
)
private val MacroColorsDark = mapOf(
    Macro.PROTEIN to Color(0xFF9085E9),
    Macro.FAT to Color(0xFFD55181),
    Macro.CARBS to Color(0xFF2E9E2E)
)

/** Reserved status color for "over the goal"; always shown with an icon and text. */
val StatusCritical = Color(0xFFD03B3B)

@Composable
@ReadOnlyComposable
fun isDarkSurface(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
@ReadOnlyComposable
fun mealColor(meal: MealType): Color = (if (isDarkSurface()) MealColorsDark else MealColorsLight).getValue(meal)

@Composable
@ReadOnlyComposable
fun macroColor(macro: Macro): Color = (if (isDarkSurface()) MacroColorsDark else MacroColorsLight).getValue(macro)
