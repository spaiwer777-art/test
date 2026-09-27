package com.example.calorietracker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.example.calorietracker.ui.components.AppTextField
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.data.ActivityLevel
import com.example.calorietracker.data.Calc
import com.example.calorietracker.data.Profile
import com.example.calorietracker.data.Sex
import com.example.calorietracker.data.WeightGoal
import com.example.calorietracker.ui.components.AnimatedNumber
import com.example.calorietracker.ui.components.IconBadge
import com.example.calorietracker.ui.components.MacroDonut
import com.example.calorietracker.ui.components.SectionCard
import com.example.calorietracker.ui.components.formatGrams
import com.example.calorietracker.ui.components.formatOneDecimal
import com.example.calorietracker.ui.components.toNumberOrNull
import com.example.calorietracker.viewmodel.CalculatorsViewModel
import kotlin.math.roundToInt

enum class CalcType(val title: String, val subtitle: String, val icon: ImageVector) {
    CALORIES("Норма калорий", "Сколько есть, чтобы худеть, держать вес или набирать", Icons.Filled.LocalFireDepartment),
    MACROS("БЖУ под цель", "Белки, жиры и углеводы в граммах", Icons.Filled.PieChart),
    BMI("Индекс массы тела", "ИМТ и здоровый диапазон веса", Icons.Filled.MonitorWeight),
    WATER("Норма воды", "Сколько пить в день", Icons.Filled.WaterDrop),
    BODY_FAT("Процент жира", "Метод ВМС США по обхватам", Icons.Filled.Straighten),
    IDEAL_WEIGHT("Идеальный вес", "Формулы Девайна, Робинсона, Миллера", Icons.Filled.Accessibility),
    EXERCISE("Калории тренировки", "Сколько сжигает активность", Icons.Filled.DirectionsRun)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorsScreen(onOpen: (CalcType) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Калькуляторы") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalcType.entries.forEach { t ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(Modifier.clickable { onOpen(t) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(t.icon, MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.title, style = MaterialTheme.typography.titleMedium)
                            Text(t.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(
                "Результаты — ориентир для здоровых взрослых, а не медицинская рекомендация.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun CalculatorScreen(type: CalcType, onBack: () -> Unit) {
    val vm: CalculatorsViewModel = viewModel()
    val saved by vm.profile.collectAsState()
    CalculatorContent(type, saved, vm::saveProfile, vm::setCalorieGoal, vm::logWeight, onBack)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CalculatorContent(
    type: CalcType,
    saved: Profile,
    onSaveProfile: (Profile) -> Unit,
    onSetGoal: (Double) -> Unit,
    onLogWeight: (Double) -> Unit,
    onBack: () -> Unit
) {
    var p by remember { mutableStateOf(saved) }
    // Pick up the stored profile once it has loaded.
    LaunchedEffect(saved) { p = saved }
    var done by remember { mutableStateOf<String?>(null) }

    val needsActivity = type in setOf(CalcType.CALORIES, CalcType.MACROS, CalcType.WATER)
    val needsGoal = type in setOf(CalcType.CALORIES, CalcType.MACROS)
    val needsAge = type in setOf(CalcType.CALORIES, CalcType.MACROS)
    val needsHeight = type != CalcType.WATER && type != CalcType.EXERCISE
    val needsWeight = type != CalcType.IDEAL_WEIGHT && type != CalcType.BODY_FAT

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(type.title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard(title = "Твои данные") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Sex.entries.forEachIndexed { i, s ->
                        SegmentedButton(
                            selected = p.sex == s,
                            onClick = { p = p.copy(sex = s) },
                            shape = SegmentedButtonDefaults.itemShape(i, Sex.entries.size)
                        ) { Text(s.label) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (needsAge) NumField("Возраст", p.age.toDouble(), Modifier.weight(1f), 14.0..100.0, decimals = false) { p = p.copy(age = it.roundToInt()) }
                    if (needsHeight) NumField("Рост, см", p.heightCm, Modifier.weight(1f), 120.0..230.0) { p = p.copy(heightCm = it) }
                    if (needsWeight) NumField("Вес, кг", p.weightKg, Modifier.weight(1f), 30.0..300.0) { p = p.copy(weightKg = it) }
                }
                if (needsActivity) {
                    Spacer(Modifier.height(12.dp))
                    Text("Активность", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActivityLevel.entries.forEach { a ->
                            FilterChip(selected = p.activity == a, onClick = { p = p.copy(activity = a) }, label = { Text(a.label) })
                        }
                    }
                    Text(p.activity.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (needsGoal) {
                    Spacer(Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        WeightGoal.entries.forEachIndexed { i, g ->
                            SegmentedButton(
                                selected = p.goal == g,
                                onClick = { p = p.copy(goal = g) },
                                shape = SegmentedButtonDefaults.itemShape(i, WeightGoal.entries.size)
                            ) {
                                Text(
                                    when (g) {
                                        WeightGoal.LOSE -> "Худеть"
                                        WeightGoal.MAINTAIN -> "Держать"
                                        WeightGoal.GAIN -> "Набрать"
                                    },
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { onSaveProfile(p); done = "Данные сохранены" }) { Text("Сохранить мои данные") }
            }

            when (type) {
                CalcType.CALORIES -> CaloriesResult(p) { kcal -> onSaveProfile(p); onSetGoal(kcal); done = "Цель ${kcal.roundToInt()} ккал установлена" }
                CalcType.MACROS -> MacrosResult(p)
                CalcType.BMI -> BmiResult(p) { onLogWeight(p.weightKg); done = "Вес записан" }
                CalcType.WATER -> WaterResult(p)
                CalcType.BODY_FAT -> BodyFatResult(p)
                CalcType.IDEAL_WEIGHT -> IdealWeightResult(p)
                CalcType.EXERCISE -> ExerciseResult(p)
            }
            done?.let { Text("✓ $it", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NumField(
    label: String,
    value: Double,
    modifier: Modifier,
    range: ClosedFloatingPointRange<Double>? = null,
    decimals: Boolean = true,
    onValue: (Double) -> Unit
) = com.example.calorietracker.ui.components.NumberField(label, value, onValue, modifier, range = range, decimals = decimals)

@Composable
private fun BigResult(value: Int, unit: String, caption: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        AnimatedNumber(value, MaterialTheme.typography.displaySmall, MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text(unit, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
    }
    Text(caption, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ResultRow(label: String, value: String, strong: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun CaloriesResult(p: Profile, onSetGoal: (Double) -> Unit) {
    val target = Calc.calorieTarget(p)
    SectionCard(title = "Результат", subtitle = "Формула Миффлина — Сан Жеора") {
        BigResult(target.roundToInt(), "ккал/день", "Цель: ${p.goal.label.lowercase()}")
        Spacer(Modifier.height(12.dp))
        ResultRow("Базовый обмен (в покое)", "${Calc.bmr(p).roundToInt()} ккал")
        ResultRow("С учётом активности", "${Calc.tdee(p).roundToInt()} ккал")
        WeightGoal.entries.forEach { g ->
            ResultRow(g.label, "${Calc.calorieTarget(p.copy(goal = g)).roundToInt()} ккал", strong = g == p.goal)
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onSetGoal(target.roundToInt().toDouble()) }, modifier = Modifier.fillMaxWidth()) {
            Text("Сделать целью в дневнике")
        }
    }
}

@Composable
private fun MacrosResult(p: Profile) {
    val kcal = Calc.calorieTarget(p)
    val m = Calc.macros(p, kcal)
    SectionCard(title = "Результат", subtitle = "Для ${kcal.roundToInt()} ккал в день") {
        MacroDonut(m.protein, m.fat, m.carbs)
        Spacer(Modifier.height(12.dp))
        ResultRow("Белки", "${m.protein.roundToInt()} г · ${formatGrams(m.protein / p.weightKg)} г/кг")
        ResultRow("Жиры", "${m.fat.roundToInt()} г · ${formatGrams(m.fat / p.weightKg)} г/кг")
        ResultRow("Углеводы", "${m.carbs.roundToInt()} г")
        Spacer(Modifier.height(8.dp))
        Text(
            "Белок выше при похудении — чтобы сохранить мышцы. Жиры — не меньше 0,9 г/кг.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Status colors (reserved for good/warning/serious/critical) always paired with a text label.
private val StatusGood = Color(0xFF0CA30C)
private val StatusWarning = Color(0xFFFAB219)
private val StatusSerious = Color(0xFFEC835A)
private val StatusCriticalC = Color(0xFFD03B3B)

@Composable
private fun BmiResult(p: Profile, onLogWeight: () -> Unit) {
    val bmi = Calc.bmi(p.weightKg, p.heightCm)
    val cls = Calc.bmiClass(bmi)
    val range = Calc.healthyWeightRange(p.heightCm)
    SectionCard(title = "Результат") {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(formatOneDecimal(bmi, floor = true), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(cls.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
        }
        Spacer(Modifier.height(16.dp))
        BmiGauge(bmi)
        Spacer(Modifier.height(16.dp))
        ResultRow("Здоровый вес для роста ${p.heightCm.roundToInt()} см", "${range.start.roundToInt()}–${range.endInclusive.roundToInt()} кг")
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onLogWeight, modifier = Modifier.fillMaxWidth()) { Text("Записать ${formatGrams(p.weightKg)} кг в дневник веса") }
    }
}

/** Horizontal scale 15–40 split into BMI classes with a marker at the value. */
@Composable
private fun BmiGauge(bmi: Double) {
    val min = 15.0
    val max = 40.0
    val zones = listOf(18.5 to StatusWarning, 25.0 to StatusGood, 30.0 to StatusSerious, max to StatusCriticalC)
    val marker by animateFloatAsState(((bmi - min) / (max - min)).toFloat().coerceIn(0f, 1f), label = "bmi")
    val ink = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.fillMaxWidth().height(28.dp)) {
        val barTop = 12.dp.toPx()
        val barH = 10.dp.toPx()
        val gap = 2.dp.toPx()
        var from = min
        zones.forEach { (to, color) ->
            val x0 = ((from - min) / (max - min)).toFloat() * size.width
            val x1 = ((to - min) / (max - min)).toFloat() * size.width
            drawRoundRect(color, Offset(x0, barTop), Size(x1 - x0 - gap, barH), CornerRadius(4.dp.toPx()))
            from = to
        }
        val x = marker * size.width
        val tri = Path().apply {
            moveTo(x, barTop - 2.dp.toPx())
            lineTo(x - 6.dp.toPx(), 0f)
            lineTo(x + 6.dp.toPx(), 0f)
            close()
        }
        drawPath(tri, ink)
    }
    Row(Modifier.fillMaxWidth()) {
        listOf("<18,5", "18,5–25", "25–30", "30+").forEach {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun WaterResult(p: Profile) {
    val ml = Calc.waterMl(p.weightKg, p.activity)
    SectionCard(title = "Результат") {
        BigResult(ml, "мл/день", "≈ ${(ml / 250.0).roundToInt()} стаканов по 250 мл")
        Spacer(Modifier.height(8.dp))
        Text(
            "30 мл на кг веса плюс запас на тренировки. В жару и при болезни нужно больше. Эта же норма используется в дневнике.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BodyFatResult(p: Profile) {
    var neck by remember { mutableStateOf(if (p.sex == Sex.MALE) 38.0 else 33.0) }
    var waist by remember { mutableStateOf(if (p.sex == Sex.MALE) 85.0 else 72.0) }
    var hip by remember { mutableStateOf(96.0) }
    SectionCard(title = "Обхваты", subtitle = "Шея — под кадыком, талия — на уровне пупка, бёдра — по самой широкой части") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumField("Шея, см", neck, Modifier.weight(1f)) { neck = it }
            NumField("Талия, см", waist, Modifier.weight(1f)) { waist = it }
            if (p.sex == Sex.FEMALE) NumField("Бёдра, см", hip, Modifier.weight(1f)) { hip = it }
        }
    }
    val fat = Calc.bodyFatNavy(p.sex, p.heightCm, neck, waist, hip)
    SectionCard(title = "Результат") {
        if (fat == null) {
            Text("Проверь обхваты — с такими значениями формула не работает.")
        } else {
            val bands = if (p.sex == Sex.MALE) listOf(6.0 to "Необходимый минимум", 14.0 to "Атлетичный", 18.0 to "Спортивный", 25.0 to "Средний", 100.0 to "Выше нормы")
            else listOf(14.0 to "Необходимый минимум", 21.0 to "Атлетичный", 25.0 to "Спортивный", 32.0 to "Средний", 100.0 to "Выше нормы")
            BigResult(fat.roundToInt(), "% жира", bands.first { fat < it.first }.second)
            Spacer(Modifier.height(8.dp))
            Text("Точность метода ±3–4%. Измеряй утром, в одном и том же месте.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun IdealWeightResult(p: Profile) {
    val range = Calc.healthyWeightRange(p.heightCm)
    SectionCard(title = "Результат", subtitle = "Для роста ${p.heightCm.roundToInt()} см") {
        Calc.idealWeights(p.sex, p.heightCm).forEach { (name, kg) -> ResultRow(name, "${formatGrams(kg)} кг") }
        ResultRow("Здоровый диапазон (ИМТ 18,5–25)", "${range.start.roundToInt()}–${range.endInclusive.roundToInt()} кг", strong = true)
        Spacer(Modifier.height(8.dp))
        Text("Формулы не учитывают мышцы и телосложение — ориентируйся на диапазон.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExerciseResult(p: Profile) {
    var exercise by remember { mutableStateOf(Calc.exercises.first()) }
    var minutes by remember { mutableStateOf(30.0) }
    SectionCard(title = "Тренировка") {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Calc.exercises.forEach { e -> FilterChip(selected = e == exercise, onClick = { exercise = e }, label = { Text(e.name) }) }
        }
        Spacer(Modifier.height(12.dp))
        NumField("Минут", minutes, Modifier.fillMaxWidth(), 0.0..600.0) { minutes = it }
    }
    val kcal = Calc.exerciseKcal(exercise.met, p.weightKg, minutes)
    SectionCard(title = "Результат") {
        BigResult(kcal.roundToInt(), "ккал", "${exercise.name}, ${minutes.roundToInt()} мин при весе ${formatGrams(p.weightKg)} кг")
        Spacer(Modifier.height(8.dp))
        Text("Расчёт по метаболическому эквиваленту (MET = ${formatGrams(exercise.met)}).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
