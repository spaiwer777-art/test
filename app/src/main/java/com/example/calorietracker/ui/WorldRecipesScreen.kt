package com.example.calorietracker.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.example.calorietracker.ui.components.AppTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.calorietracker.network.MealDetails
import com.example.calorietracker.network.MealSummary
import com.example.calorietracker.ui.components.EmptyState
import com.example.calorietracker.ui.components.Pill
import com.example.calorietracker.ui.components.SectionCard
import com.example.calorietracker.ui.components.formatGrams
import com.example.calorietracker.viewmodel.MEALDB_AREAS
import com.example.calorietracker.viewmodel.MEALDB_CATEGORIES
import com.example.calorietracker.viewmodel.TranslateState
import com.example.calorietracker.viewmodel.WorldListState
import com.example.calorietracker.viewmodel.WorldRecipeViewModel
import com.example.calorietracker.viewmodel.WorldRecipesViewModel
import kotlin.math.roundToInt

/** Grid of TheMealDB recipes with cuisine/category filters; shown in the Recipes tab. */
@Composable
fun WorldRecipesPane(onOpen: (String) -> Unit, header: @Composable () -> Unit) {
    val vm: WorldRecipesViewModel = viewModel()
    val state by vm.state.collectAsState()
    val filter by vm.filter.collectAsState()
    val query by vm.query.collectAsState()
    WorldRecipesGrid(state, filter, query, vm::select, vm::setQuery, vm::load, onOpen, header)
}

@Composable
internal fun WorldRecipesGrid(
    state: WorldListState,
    filter: String,
    query: String,
    onSelect: (String) -> Unit,
    onQuery: (String) -> Unit,
    onRetry: () -> Unit,
    onOpen: (String) -> Unit,
    header: @Composable () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(2) }) { header() }
        item(span = { GridItemSpan(2) }) {
            AppTextField(
                value = query,
                onValueChange = onQuery,
                placeholder = { Text("Поиск по-английски: soup, chicken, cake…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item(span = { GridItemSpan(2) }) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MEALDB_AREAS.forEach { (key, label) ->
                        FilterChip(selected = filter == "a:$key", onClick = { onSelect("a:$key") }, label = { Text(label) })
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MEALDB_CATEGORIES.forEach { (key, label) ->
                        FilterChip(selected = filter == "c:$key", onClick = { onSelect("c:$key") }, label = { Text(label) })
                    }
                }
            }
        }
        when (state) {
            WorldListState.Loading -> item(span = { GridItemSpan(2) }) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            is WorldListState.Error -> item(span = { GridItemSpan(2) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    EmptyState(Icons.Filled.Public, state.message)
                    TextButton(onClick = onRetry) { Text("Повторить") }
                }
            }
            is WorldListState.Loaded -> {
                if (state.meals.isEmpty()) item(span = { GridItemSpan(2) }) { EmptyState(Icons.Filled.Search, "Ничего не найдено") }
                items(state.meals, key = { it.id }) { m -> MealTile(m) { onOpen(m.id) } }
                item(span = { GridItemSpan(2) }) {
                    Text(
                        "Рецепты и фото — TheMealDB (themealdb.com), тексты на английском. ИИ переведёт и посчитает КБЖУ.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MealTile(meal: MealSummary, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        AsyncImage(
            model = meal.thumb?.let { "$it/preview" },
            contentDescription = meal.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Text(
            meal.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldRecipeScreen(onSaved: (Long) -> Unit, onOpenSettings: () -> Unit, onBack: () -> Unit) {
    val vm: WorldRecipeViewModel = viewModel()
    val meal by vm.meal.collectAsState()
    val error by vm.error.collectAsState()
    val translate by vm.translate.collectAsState()
    val savedId by vm.savedId.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Рецепт") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } }
            )
        }
    ) { padding ->
        val m = meal
        when {
            m == null && error != null -> EmptyState(Icons.Filled.Public, error!!, Modifier.padding(padding))
            m == null -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else -> WorldRecipeContent(
                meal = m,
                translate = translate,
                savedId = savedId,
                onTranslate = vm::translate,
                onSave = { vm.save(onSaved) },
                onOpenSaved = { savedId?.let(onSaved) },
                onOpenSettings = onOpenSettings,
                onYoutube = { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
internal fun WorldRecipeContent(
    meal: MealDetails,
    translate: TranslateState,
    savedId: Long?,
    onTranslate: () -> Unit,
    onSave: () -> Unit,
    onOpenSaved: () -> Unit,
    onOpenSettings: () -> Unit,
    onYoutube: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box {
            AsyncImage(
                model = meal.thumb,
                contentDescription = meal.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.3f).background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.65f))
                )
            )
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text(meal.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                val tags = listOfNotNull(
                    meal.area?.let { a -> MEALDB_AREAS.firstOrNull { it.first == a }?.second ?: a },
                    meal.category?.let { c -> MEALDB_CATEGORIES.firstOrNull { it.first == c }?.second ?: c }
                )
                Text(tags.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (translate) {
                is TranslateState.Done -> TranslatedCard(translate, savedId, onSave, onOpenSaved)
                else -> SectionCard(title = "По-русски и с КБЖУ", subtitle = "ИИ переведёт рецепт, пересчитает меры в граммы и посчитает калории") {
                    when (translate) {
                        TranslateState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Перевожу и считаю… 10–20 секунд")
                        }
                        TranslateState.NoApiKey -> Column {
                            Text("Нужен ключ Groq в настройках.")
                            TextButton(onClick = onOpenSettings) { Text("Открыть настройки") }
                        }
                        is TranslateState.Error -> Column {
                            Text(translate.message, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = onTranslate) { Text("Повторить") }
                        }
                        else -> Button(onClick = onTranslate, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            Icon(Icons.Filled.AutoAwesome, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Перевести и посчитать КБЖУ")
                        }
                    }
                    if (savedId != null) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onOpenSaved, modifier = Modifier.fillMaxWidth()) { Text("Уже в моих рецептах — открыть") }
                    }
                }
            }

            SectionCard(title = "Ingredients", subtitle = "Оригинал") {
                meal.ingredients.forEach { ing ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        Spacer(Modifier.width(12.dp))
                        Text(ing.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(ing.measure, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            SectionCard(title = "Instructions") {
                Text(meal.instructions, style = MaterialTheme.typography.bodyMedium)
            }
            meal.youtube?.let { url ->
                OutlinedButton(onClick = { onYoutube(url) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.PlayCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Видео на YouTube")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TranslatedCard(state: TranslateState.Done, savedId: Long?, onSave: () -> Unit, onOpenSaved: () -> Unit) {
    val r = state.recipe
    val items = state.items
    val servings = (r.servings ?: 4).coerceAtLeast(1)
    val total = items.sumOf { it.calories }
    SectionCard(title = r.name ?: "", subtitle = "$servings порц. · ${r.minutes ?: "?"} мин · ${r.category ?: ""}", containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Порция", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("${(total / servings).roundToInt()} ккал", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(Modifier.weight(1f)) {
                Text("Всё блюдо", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("${total.roundToInt()} ккал", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Text(
            "Б ${(items.sumOf { it.protein } / servings).roundToInt()} · Ж ${(items.sumOf { it.fat } / servings).roundToInt()} · У ${(items.sumOf { it.carbs } / servings).roundToInt()} г на порцию",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(Modifier.height(12.dp))
        items.forEach { ing ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(ing.name + if (ing.baseName != null) " ✓" else "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
                Text("${formatGrams(ing.grams)} г", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        r.steps?.takeIf { it.isNotEmpty() }?.let { steps ->
            Spacer(Modifier.height(12.dp))
            steps.forEachIndexed { i, s ->
                Text("${i + 1}. $s", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(vertical = 3.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        if (savedId == null) {
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Сохранить в мои рецепты") }
        } else {
            Row {
                Pill("✓ Сохранено")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onOpenSaved) { Text("Открыть") }
            }
        }
    }
}
