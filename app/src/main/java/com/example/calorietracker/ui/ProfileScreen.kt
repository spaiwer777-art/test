package com.example.calorietracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.data.ActivityLevel
import com.example.calorietracker.data.Calc
import com.example.calorietracker.data.MacroSplit
import com.example.calorietracker.data.Sex
import com.example.calorietracker.data.WeightGoal
import com.example.calorietracker.ui.components.AnimatedNumber
import com.example.calorietracker.ui.components.NumberField
import com.example.calorietracker.ui.components.SectionCard
import com.example.calorietracker.ui.components.formatOneDecimal
import com.example.calorietracker.ui.theme.isDarkSurface
import com.example.calorietracker.viewmodel.MacroGoals
import com.example.calorietracker.viewmodel.ProfileUiState
import com.example.calorietracker.viewmodel.ProfileViewModel
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val vm: ProfileViewModel = viewModel()
    val state by vm.state.collectAsState()
    ProfileContent(
        state = state,
        onBack = onBack,
        onProfile = vm::update,
        onCalorieGoal = vm::setCalorieGoal,
        onCalorieAuto = vm::setCalorieAuto,
        onWaterGoal = vm::setWaterGoal
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ProfileContent(
    state: ProfileUiState?,
    onBack: () -> Unit,
    onProfile: (com.example.calorietracker.data.Profile) -> Unit,
    onCalorieGoal: (Double) -> Unit,
    onCalorieAuto: (Boolean) -> Unit,
    onWaterGoal: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("О себе") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } }
            )
        }
    ) { padding ->
        if (state == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val p = state.profile
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Hero(state)

            SectionCard(title = "Тело", subtitle = "Всё сохраняется сразу") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Sex.entries.forEachIndexed { i, s ->
                        SegmentedButton(
                            selected = p.sex == s, onClick = { onProfile(p.copy(sex = s)) },
                            shape = SegmentedButtonDefaults.itemShape(i, Sex.entries.size)
                        ) { Text(s.label) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(
                        "Возраст", p.age.toDouble(), { onProfile(p.copy(age = it.roundToInt())) }, Modifier.weight(1f),
                        unit = "лет", range = 14.0..100.0, decimals = false, icon = Icons.Filled.Cake
                    )
                    NumberField(
                        "Рост", p.heightCm, { onProfile(p.copy(heightCm = it)) }, Modifier.weight(1f),
                        unit = "см", range = 120.0..230.0, icon = Icons.Filled.Height
                    )
                }
                Spacer(Modifier.height(10.dp))
                NumberField(
                    "Вес", p.weightKg, { onProfile(p.copy(weightKg = it)) }, Modifier.fillMaxWidth(),
                    unit = "кг", range = 30.0..300.0, icon = Icons.Filled.MonitorWeight, imeAction = ImeAction.Done
                )
            }

            SectionCard(title = "Активность") {
                ActivityLevel.entries.forEach { a ->
                    val selected = p.activity == a
                    Card(
                        onClick = { onProfile(p.copy(activity = a)) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(a.label, style = MaterialTheme.typography.titleSmall)
                                Text(a.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (selected) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            SectionCard(title = "Цель") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeightGoal.entries.forEach { g ->
                        FilterChip(selected = p.goal == g, onClick = { onProfile(p.copy(goal = g)) }, label = { Text(g.label) })
                    }
                }
            }

            SectionCard(
                title = "Норма калорий",
                action = { BadgeIcon(Icons.Filled.LocalFireDepartment, Color(0xFFEB6834)) }
            ) {
                ToggleRow(
                    "Считать автоматически",
                    "По профилю" + (state.dietName?.let { " и диете «$it»" } ?: "") + ": ${state.suggestedCalories.roundToInt()} ккал",
                    state.calorieAuto, onCalorieAuto
                )
                Spacer(Modifier.height(10.dp))
                NumberField(
                    "Ккал в день", state.dailyGoal, onCalorieGoal, Modifier.fillMaxWidth(),
                    unit = "ккал", range = 800.0..6000.0, decimals = false, enabled = !state.calorieAuto,
                    icon = Icons.Filled.LocalFireDepartment
                )
                val m = MacroGoals.fromCalories(state.dailyGoal, MacroSplit.DEFAULT)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Примерно: белки ${m.protein.roundToInt()} г · жиры ${m.fat.roundToInt()} г · углеводы ${m.carbs.roundToInt()} г. " +
                        "Свои граммы БЖУ — на главной, кнопка «Изменить».",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val water = if (isDarkSurface()) Color(0xFF3987E5) else Color(0xFF2A78D6)
            SectionCard(title = "Норма воды", action = { BadgeIcon(Icons.Filled.WaterDrop, water) }) {
                ToggleRow(
                    "Считать автоматически",
                    "30 мл на кг веса + активность: ${state.suggestedWaterMl} мл",
                    state.ownWaterMl == 0
                ) { auto -> onWaterGoal(if (auto) 0 else state.suggestedWaterMl) }
                Spacer(Modifier.height(10.dp))
                NumberField(
                    "Воды в день", state.waterGoalMl.toDouble(), { onWaterGoal(it.roundToInt()) }, Modifier.fillMaxWidth(),
                    unit = "мл", range = 500.0..6000.0, decimals = false, enabled = state.ownWaterMl > 0,
                    icon = Icons.Filled.WaterDrop, imeAction = ImeAction.Done
                )
                AnimatedVisibility(state.ownWaterMl > 0) {
                    FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1500, 2000, 2500, 3000).forEach { ml ->
                            FilterChip(
                                selected = state.ownWaterMl == ml, onClick = { onWaterGoal(ml) },
                                label = { Text("${formatOneDecimal(ml / 1000.0)} л") }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Hero(state: ProfileUiState) {
    val p = state.profile
    val bmi = Calc.bmi(p.weightKg, p.heightCm)
    val bg = Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f))
    )
    Card(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.background(bg).padding(vertical = 18.dp, horizontal = 8.dp)) {
            HeroStat(state.dailyGoal.roundToInt(), "ккал в день", Modifier.weight(1f))
            HeroStat(state.waterGoalMl, "мл воды", Modifier.weight(1f))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatOneDecimal(bmi), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text("ИМТ · ${Calc.bmiClass(bmi).label.lowercase()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun HeroStat(value: Int, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedNumber(value, MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun BadgeIcon(icon: ImageVector, color: Color) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
    }
}
