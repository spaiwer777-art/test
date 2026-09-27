package com.example.calorietracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.data.Calc
import com.example.calorietracker.data.Diet
import com.example.calorietracker.data.DietRules
import com.example.calorietracker.data.Profile
import com.example.calorietracker.data.split
import com.example.calorietracker.ui.components.IconBadge
import com.example.calorietracker.ui.components.MacroDonut
import com.example.calorietracker.ui.components.Pill
import com.example.calorietracker.ui.components.SectionCard
import com.example.calorietracker.viewmodel.DietDetailsViewModel
import com.example.calorietracker.viewmodel.DietEditorViewModel
import com.example.calorietracker.viewmodel.DietsViewModel
import com.example.calorietracker.viewmodel.MacroGoals
import com.example.calorietracker.viewmodel.dietCalories
import kotlin.math.roundToInt

private fun adjustText(pct: Int) = when {
    pct == 0 -> "поддержание"
    pct > 0 -> "+$pct% ккал"
    else -> "−${-pct}% ккал"
}

@Composable
fun DietsScreen(onOpen: (Long) -> Unit, onCreate: () -> Unit, onBack: () -> Unit) {
    val vm: DietsViewModel = viewModel()
    val diets by vm.diets.collectAsState()
    val activeId by vm.activeId.collectAsState()
    DietsContent(diets, activeId, onOpen, onCreate, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DietsContent(diets: List<Diet>, activeId: Long, onOpen: (Long) -> Unit, onCreate: () -> Unit, onBack: () -> Unit) {
    val active = diets.firstOrNull { it.id == activeId }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Диеты") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onCreate, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Своя диета") })
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SectionCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.Eco, MaterialTheme.colorScheme.primary, 48.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (active != null) "Сейчас: ${active.name}" else "Диета не выбрана",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                if (active != null) "Нормы БЖУ в дневнике: Б ${active.proteinPct}% · Ж ${active.fatPct}% · У ${active.carbsPct}%"
                                else "Выбери диету — нормы БЖУ, рацион и подсказки подстроятся под неё",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            items(diets, key = { it.id }) { d -> DietCard(d, d.id == activeId, Modifier.animateItem()) { onOpen(d.id) } }
        }
    }
}

@Composable
private fun DietCard(diet: Diet, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.clickable(onClick = onClick).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(diet.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (active) {
                    Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("активна", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                } else if (!diet.isBuiltin) {
                    Pill("Моя")
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(diet.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val g = MacroGoals.fromCalories(100.0, diet.split)
                MacroSplitLine(g.protein, g.fat, g.carbs, Modifier.width(120.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Б ${diet.proteinPct} · Ж ${diet.fatPct} · У ${diet.carbsPct}% · ${adjustText(diet.calorieAdjustPct)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DietDetailsScreen(onEdit: (Long) -> Unit, onBack: () -> Unit) {
    val vm: DietDetailsViewModel = viewModel()
    val diet by vm.diet.collectAsState()
    val active by vm.isActive.collectAsState()
    val profile by vm.profile.collectAsState()
    val d = diet
    if (d == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    DietDetailsContent(d, active, profile, vm::start, vm::stop, { onEdit(d.id) }, if (!d.isBuiltin) ({ vm.delete(onBack) }) else null, onBack)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun DietDetailsContent(
    diet: Diet,
    active: Boolean,
    profile: Profile,
    onStart: (setCalories: Boolean) -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit
) {
    val kcal = dietCalories(profile, diet)
    val goals = MacroGoals.fromCalories(kcal, diet.split)
    var setCalories by remember { mutableStateOf(true) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Диета") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(if (diet.isBuiltin) Icons.Filled.ContentCopy else Icons.Filled.Edit, if (diet.isBuiltin) "Создать свою на основе" else "Изменить")
                    }
                    if (onDelete != null) IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Удалить") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard {
                Text(diet.name, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text(diet.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(adjustText(diet.calorieAdjustPct))
                    Pill("${diet.mealsPerDay} приёма пищи")
                    if (active) Pill("активна")
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${kcal.roundToInt()}", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text("ккал в день для тебя", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 6.dp))
                }
                Text(
                    "Поддержание ${Calc.tdee(profile).roundToInt()} ккал ${adjustText(diet.calorieAdjustPct).let { if (diet.calorieAdjustPct == 0) "" else "($it)" }} — по данным из калькулятора",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                MacroDonut(goals.protein, goals.fat, goals.carbs)
            }

            SectionCard {
                if (active) {
                    Text("Диета активна: нормы БЖУ в дневнике, рацион и подсказки учитывают её.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("Завершить диету") }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = setCalories, onCheckedChange = { setCalories = it })
                        Text("Сделать ${kcal.roundToInt()} ккал целью в дневнике", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onStart(setCalories) }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Начать диету") }
                }
            }

            val tips = diet.recommended.lines().filter { it.isNotBlank() }
            if (tips.isNotEmpty()) {
                SectionCard(title = "Как питаться") {
                    tips.forEach { tip ->
                        Row(Modifier.padding(vertical = 6.dp)) {
                            Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(tip, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            val avoid = DietRules.keywords(diet).map { it.removeSuffix("=") }
            if (avoid.isNotEmpty()) {
                SectionCard(title = "Лучше избегать", subtitle = "Продукты с такими словами в названии приложение пометит") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        avoid.take(40).forEach { SuggestionChip(onClick = {}, label = { Text(it) }, icon = { Icon(Icons.Outlined.Block, null, Modifier.size(16.dp)) }) }
                    }
                }
            }
            Text(
                "Диеты — общие рекомендации для здоровых взрослых. При заболеваниях, беременности и для лечебных диет посоветуйся с врачом.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietEditorScreen(onSaved: (Long) -> Unit, onBack: () -> Unit) {
    val vm: DietEditorViewModel = viewModel()
    val name by vm.name.collectAsState()
    val description by vm.description.collectAsState()
    val protein by vm.protein.collectAsState()
    val fat by vm.fat.collectAsState()
    val adjust by vm.adjust.collectAsState()
    val meals by vm.meals.collectAsState()
    val recommended by vm.recommended.collectAsState()
    val avoid by vm.avoid.collectAsState()
    val carbs = 100 - protein - fat

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isEditing) "Изменить диету" else "Своя диета") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard(title = "Основное") {
                OutlinedTextField(name, { vm.name.value = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(description, { vm.description.value = it }, label = { Text("Описание") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
            SectionCard(title = "БЖУ", subtitle = "Доля калорий. Углеводы — остаток до 100%") {
                PercentSlider("Белки", protein, 10..60) { vm.protein.value = it.coerceAtMost(100 - fat) }
                PercentSlider("Жиры", fat, 10..80) { vm.fat.value = it.coerceAtMost(100 - protein) }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text("Углеводы", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text("$carbs%", style = MaterialTheme.typography.titleMedium, color = if (carbs < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(12.dp))
                val g = MacroGoals.fromCalories(2000.0, com.example.calorietracker.data.MacroSplit(protein, fat, carbs.coerceAtLeast(0)))
                MacroDonut(g.protein, g.fat, g.carbs)
                Text("Граммы — для 2000 ккал", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SectionCard(title = "Калории", subtitle = "Изменение к норме поддержания") {
                Text(adjustText(adjust), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Slider(value = adjust.toFloat(), onValueChange = { vm.adjust.value = (it / 5).roundToInt() * 5 }, valueRange = -30f..20f, steps = 9)
                Text("Приёмов пищи", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (2..6).forEach { n -> FilterChip(selected = meals == n, onClick = { vm.meals.value = n }, label = { Text("$n") }) }
                }
            }
            SectionCard(title = "Правила", subtitle = "Каждый совет — с новой строки") {
                OutlinedTextField(recommended, { vm.recommended.value = it }, minLines = 3, modifier = Modifier.fillMaxWidth())
            }
            SectionCard(title = "Исключить продукты", subtitle = "Через запятую: сахар, хлеб, картоф… Слово ищется в начале слов названия") {
                OutlinedTextField(
                    avoid, { vm.avoid.value = it }, minLines = 2, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions()
                )
            }
            Button(
                onClick = { vm.save(onSaved) },
                enabled = name.isNotBlank() && carbs >= 0,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Сохранить диету") }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PercentSlider(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text("$value%", style = MaterialTheme.typography.titleMedium)
    }
    Slider(
        value = value.toFloat(),
        onValueChange = { onChange(it.roundToInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat()
    )
}
