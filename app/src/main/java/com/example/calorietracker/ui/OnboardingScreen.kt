package com.example.calorietracker.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.example.calorietracker.ui.components.AppTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.data.ActivityLevel
import com.example.calorietracker.data.Diet
import com.example.calorietracker.data.Profile
import com.example.calorietracker.data.Sex
import com.example.calorietracker.data.WeightGoal
import com.example.calorietracker.data.split
import com.example.calorietracker.ui.components.AnimatedNumber
import com.example.calorietracker.ui.components.MacroDonut
import com.example.calorietracker.ui.components.NumberField
import com.example.calorietracker.ui.components.formatGrams
import com.example.calorietracker.ui.components.toNumberOrNull
import com.example.calorietracker.viewmodel.AccountViewModel
import com.example.calorietracker.viewmodel.MacroGoals
import com.example.calorietracker.viewmodel.OnboardingViewModel
import kotlin.math.roundToInt

private const val STEPS = 3

/** Account first (sign up / sign in / skip), then the profile questions. */
@Composable
fun OnboardingScreen(onDone: () -> Unit, onRestore: () -> Unit) {
    val vm: OnboardingViewModel = viewModel()
    val account: AccountViewModel = viewModel()
    val profile by vm.profile.collectAsState()
    val dietId by vm.dietId.collectAsState()
    val diets by vm.diets.collectAsState()
    val session by account.session.collectAsState()
    val status by account.status.collectAsState()
    val hasCloudData by account.cloudHasData.collectAsState()
    val context = LocalContext.current
    var questions by rememberSaveable { mutableStateOf(false) }

    AnimatedContent(
        targetState = questions,
        transitionSpec = {
            val dir = if (targetState) 1 else -1
            (slideInHorizontally { it * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir } + fadeOut())
        },
        label = "stage"
    ) { q ->
        if (!q) {
            AuthContent(
                cloudConfigured = account.cloudConfigured,
                googleEnabled = account.googleEnabled,
                status = status,
                signedIn = session?.let { SignedIn(it.email, hasCloudData) },
                onSignIn = account::signIn,
                onSignUp = account::signUp,
                onGoogle = { account.signInWithGoogle(context) },
                onDownload = { account.download { vm.skip(onDone) } },
                onContinue = { questions = true },
                onRestoreFile = { vm.skip(onRestore) }
            )
        } else {
            OnboardingContent(
                profile = profile,
                onProfile = { vm.profile.value = it },
                diets = diets,
                dietId = dietId,
                onDiet = { vm.dietId.value = it },
                calories = vm.calories(),
                onFinish = { vm.finish(onDone) },
                onSkip = { vm.skip(onDone) },
                onBack = { questions = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun OnboardingContent(
    profile: Profile,
    onProfile: (Profile) -> Unit,
    diets: List<Diet>,
    dietId: Long,
    onDiet: (Long) -> Unit,
    calories: Double,
    onFinish: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    initialStep: Int = 0
) {
    var step by rememberSaveable { mutableIntStateOf(initialStep) }
    val bg = Brush.verticalGradient(
        listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), MaterialTheme.colorScheme.background)
    )
    Column(Modifier.fillMaxSize().background(bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(STEPS) { i ->
                val w by animateDpAsState(if (i == step) 28.dp else 8.dp, spring(dampingRatio = 0.6f), label = "dot")
                Box(
                    Modifier.padding(end = 6.dp).height(8.dp).width(w).clip(CircleShape)
                        .background(if (i <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                )
            }
            Spacer(Modifier.weight(1f))
            if (step < STEPS - 1) TextButton(onClick = onSkip) { Text("Пропустить") }
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally { it * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir } + fadeOut())
            },
            modifier = Modifier.weight(1f),
            label = "step"
        ) { s ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (s) {
                    0 -> AboutYou(profile, onProfile)
                    1 -> Goal(profile, onProfile, calories)
                    else -> DietChoice(diets, dietId, onDiet, calories)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { if (step > 0) step-- else onBack() }) { Text("Назад") }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { if (step < STEPS - 1) step++ else onFinish() },
                modifier = Modifier.height(52.dp)
            ) { Text(if (step < STEPS - 1) "Далее" else "Готово") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AboutYou(p: Profile, onChange: (Profile) -> Unit) {
    Text("О тебе", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.fillMaxWidth())
    Text("Нужно, чтобы рассчитать твою норму калорий и БЖУ.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(20.dp))
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        Sex.entries.forEachIndexed { i, s ->
            SegmentedButton(selected = p.sex == s, onClick = { onChange(p.copy(sex = s)) }, shape = SegmentedButtonDefaults.itemShape(i, Sex.entries.size)) { Text(s.label) }
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NumberField("Возраст", p.age.toDouble(), { onChange(p.copy(age = it.roundToInt())) }, Modifier.weight(1f), range = 14.0..100.0, decimals = false)
        NumberField("Рост, см", p.heightCm, { onChange(p.copy(heightCm = it)) }, Modifier.weight(1f), range = 120.0..230.0)
        NumberField("Вес, кг", p.weightKg, { onChange(p.copy(weightKg = it)) }, Modifier.weight(1f), range = 30.0..300.0)
    }
    Spacer(Modifier.height(20.dp))
    Text("Активность", style = MaterialTheme.typography.titleSmall, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    ActivityLevel.entries.forEach { a ->
        Card(
            onClick = { onChange(p.copy(activity = a)) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (p.activity == a) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(a.label, style = MaterialTheme.typography.titleSmall)
                    Text(a.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (p.activity == a) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun Goal(p: Profile, onChange: (Profile) -> Unit, calories: Double) {
    Text("Твоя цель", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(16.dp))
    WeightGoal.entries.forEach { g ->
        Card(
            onClick = { onChange(p.copy(goal = g)) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (p.goal == g) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(g.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (p.goal == g) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
    Spacer(Modifier.height(28.dp))
    Text("Твоя норма", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(verticalAlignment = Alignment.Bottom) {
        AnimatedNumber(calories.roundToInt(), MaterialTheme.typography.displaySmall, MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text("ккал в день", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
    }
    Text(
        "Формула Миффлина — Сан Жеора с учётом активности. Всегда можно поменять в настройках.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DietChoice(diets: List<Diet>, selected: Long, onSelect: (Long) -> Unit, calories: Double) {
    Text("Диета", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.fillMaxWidth())
    Text("Необязательно: можно выбрать позже в разделе «Ещё».", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(16.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = selected == 0L, onClick = { onSelect(0L) }, label = { Text("Без диеты") })
        diets.forEach { d -> FilterChip(selected = selected == d.id, onClick = { onSelect(d.id) }, label = { Text(d.name) }) }
    }
    Spacer(Modifier.height(20.dp))
    val diet = diets.firstOrNull { it.id == selected }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(diet?.name ?: "Сбалансированное питание", style = MaterialTheme.typography.titleMedium)
            Text(
                diet?.description ?: "Нормы БЖУ 20/30/50 — классическое сбалансированное питание.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text("${calories.roundToInt()} ккал в день", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            val g = MacroGoals.fromCalories(calories, diet?.split ?: com.example.calorietracker.data.MacroSplit.DEFAULT)
            MacroDonut(g.protein, g.fat, g.carbs)
        }
    }
}
