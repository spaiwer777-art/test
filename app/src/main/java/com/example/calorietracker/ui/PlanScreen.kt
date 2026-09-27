package com.example.calorietracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.example.calorietracker.ui.components.AppTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.data.MealType
import com.example.calorietracker.data.json.MealPlanData
import com.example.calorietracker.data.json.PlanDay
import com.example.calorietracker.data.json.PlanMeal
import com.example.calorietracker.ui.components.IconBadge
import com.example.calorietracker.ui.components.SectionCard
import com.example.calorietracker.ui.components.ThinBar
import com.example.calorietracker.ui.components.icon
import com.example.calorietracker.ui.components.label
import com.example.calorietracker.ui.theme.mealColor
import com.example.calorietracker.viewmodel.DiaryViewModel
import com.example.calorietracker.viewmodel.PlanGenState
import com.example.calorietracker.viewmodel.PlanViewModel
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun PlanScreen(onOpenRecipe: (Long) -> Unit, onOpenSettings: () -> Unit, onBack: () -> Unit) {
    val vm: PlanViewModel = viewModel()
    val diaryVm: DiaryViewModel = viewModel()
    val plan by vm.plan.collectAsState()
    val gen by vm.gen.collectAsState()
    val goal by vm.dailyGoal.collectAsState()
    val dietName by vm.dietName.collectAsState()
    val today = LocalDate.now().toEpochDay()
    PlanContent(
        plan = plan,
        gen = gen,
        dailyGoal = goal,
        dietName = dietName,
        onGenerateAi = vm::generateWithAi,
        onGenerateRecipes = vm::generateFromRecipes,
        onAddMeal = { m ->
            diaryVm.addPrecomputedEntry(
                m.dish, m.kcal, m.protein, m.fat, m.carbs, MealType.valueOf(m.meal), today,
                grams = m.grams, recipeId = m.recipeId
            )
        },
        onOpenRecipe = onOpenRecipe,
        onOpenSettings = onOpenSettings,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlanContent(
    plan: MealPlanData?,
    gen: PlanGenState,
    dailyGoal: Double,
    dietName: String?,
    onGenerateAi: (days: Int, snack: Boolean, prefs: String) -> Unit,
    onGenerateRecipes: (days: Int, snack: Boolean) -> Unit,
    onAddMeal: (PlanMeal) -> Unit,
    onOpenRecipe: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    var days by rememberSaveable { mutableIntStateOf(1) }
    var snack by rememberSaveable { mutableStateOf(true) }
    var prefs by rememberSaveable { mutableStateOf("") }
    // Meals already added to the diary in this session, to show a check instead of "+".
    var added by remember(plan) { mutableStateOf(setOf<String>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Рацион") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard(
                    title = "Составить рацион",
                    subtitle = "Цель: ${dailyGoal.roundToInt()} ккал в день" + (dietName?.let { " · диета «$it»" } ?: "")
                ) {
                    Text("Дней", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 3, 7).forEach { d -> FilterChip(selected = days == d, onClick = { days = d }, label = { Text("$d") }) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("С перекусом", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(checked = snack, onCheckedChange = { snack = it })
                    }
                    Spacer(Modifier.height(8.dp))
                    AppTextField(
                        value = prefs,
                        onValueChange = { prefs = it },
                        label = { Text("Пожелания для ИИ") },
                        placeholder = { Text("без рыбы, больше белка, готовлю 30 минут…") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onGenerateAi(days, snack, prefs) },
                        enabled = gen !is PlanGenState.Loading,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Составить с ИИ")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onGenerateRecipes(days, snack) },
                        enabled = gen !is PlanGenState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.MenuBook, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Из рецептов приложения (без ИИ)")
                    }
                    when (gen) {
                        PlanGenState.Loading -> Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator()
                            Spacer(Modifier.width(12.dp))
                            Text("ИИ составляет рацион… это 10–30 секунд", style = MaterialTheme.typography.bodyMedium)
                        }
                        PlanGenState.NoApiKey -> Column(Modifier.padding(top = 12.dp)) {
                            Text("Для ИИ нужен ключ Groq в настройках. Или собери рацион из рецептов.")
                            TextButton(onClick = onOpenSettings) { Text("Открыть настройки") }
                        }
                        is PlanGenState.Error -> Text(gen.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
                        PlanGenState.Idle -> Unit
                    }
                }
            }

            if (plan != null) {
                itemsIndexed(plan.days) { index, day ->
                    AnimatedVisibility(visible = true, enter = fadeIn() + expandVertically()) {
                        PlanDayCard(
                            index = index,
                            day = day,
                            goal = plan.calorieGoal,
                            source = plan.generatedBy,
                            added = added,
                            onAdd = { m -> onAddMeal(m); added = added + "$index-${m.meal}-${m.dish}" },
                            onAddAll = {
                                day.meals.forEach(onAddMeal)
                                added = added + day.meals.map { "$index-${it.meal}-${it.dish}" }
                            },
                            onOpenRecipe = onOpenRecipe
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanDayCard(
    index: Int,
    day: PlanDay,
    goal: Double,
    source: String,
    added: Set<String>,
    onAdd: (PlanMeal) -> Unit,
    onAddAll: () -> Unit,
    onOpenRecipe: (Long) -> Unit
) {
    SectionCard(
        title = "День ${index + 1} · ${day.calories.roundToInt()} ккал",
        subtitle = "Б ${day.protein.roundToInt()} · Ж ${day.fat.roundToInt()} · У ${day.carbs.roundToInt()} г · $source"
    ) {
        ThinBar((day.calories / goal).toFloat(), MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        day.meals.forEach { m ->
            val meal = MealType.entries.firstOrNull { it.name == m.meal } ?: MealType.SNACK
            val key = "$index-${m.meal}-${m.dish}"
            Row(
                Modifier.fillMaxWidth()
                    .then(if (m.recipeId != null) Modifier.clickable { onOpenRecipe(m.recipeId) } else Modifier)
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(meal.icon, mealColor(meal), 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(meal.label(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(m.dish, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${m.grams.roundToInt()} г · ${m.kcal.roundToInt()} ккал · Б ${m.protein.roundToInt()} Ж ${m.fat.roundToInt()} У ${m.carbs.roundToInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    m.note?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box {
                    if (key in added) {
                        Icon(Icons.Filled.Check, "Добавлено", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp))
                    } else {
                        FilledTonalIconButton(onClick = { onAdd(m) }) { Icon(Icons.Filled.Add, "Добавить в дневник на сегодня") }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onAddAll, modifier = Modifier.fillMaxWidth()) { Text("Записать весь день в дневник на сегодня") }
    }
}
