package com.example.calorietracker.ui

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.example.calorietracker.ui.components.AppTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.data.AccentColor
import com.example.calorietracker.data.ThemeMode
import com.example.calorietracker.ui.theme.isDarkSurface
import com.example.calorietracker.ui.theme.swatch
import com.example.calorietracker.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenProfile: () -> Unit) {
    val vm: SettingsViewModel = viewModel()
    val goal by vm.dailyGoal.collectAsState()
    val apiKey by vm.groqApiKey.collectAsState()
    val themeMode by vm.themeMode.collectAsState()
    val accent by vm.accent.collectAsState()
    val dynamicColor by vm.dynamicColor.collectAsState()
    val appName by vm.appName.collectAsState()

    var apiKeyText by remember(apiKey) { mutableStateOf(apiKey) }
    val keyCheck by vm.keyCheck.collectAsState()
    val checking by vm.checking.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Section("Оформление") {
                Text("Тема", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                val modes = listOf(ThemeMode.SYSTEM to "Системная", ThemeMode.LIGHT to "Светлая", ThemeMode.DARK to "Тёмная")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    modes.forEachIndexed { i, (mode, label) ->
                        SegmentedButton(
                            selected = themeMode == mode,
                            onClick = { vm.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(i, modes.size)
                        ) { Text(label) }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("Акцентный цвет", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().alpha(if (dynamicColor) 0.4f else 1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AccentColor.entries.forEach { a ->
                        AccentDot(a, selected = a == accent && !dynamicColor) {
                            vm.setDynamicColor(false)
                            vm.setAccent(a)
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Цвета из обоев", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Material You: подстроить цвета под обои телефона",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = dynamicColor, onCheckedChange = vm::setDynamicColor)
                    }
                }
            }

            Card(
                onClick = onOpenProfile,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Tune, null, tint = MaterialTheme.colorScheme.primary) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Параметры и нормы", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Рост, вес, цель · норма ${goal.roundToInt()} ккал и воды",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Section("Значок и название") {
                AppIconSection(appName, vm::setAppName)
            }

            Section("ИИ: быстрый ввод текстом") {
                Text(
                    "Нужен бесплатный API-ключ Groq (console.groq.com). Он хранится только на этом телефоне.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = com.example.calorietracker.data.cleanApiKey(it) },
                    label = { Text("Groq API-ключ") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { vm.checkGroqKey(apiKeyText) }, enabled = !checking) {
                        if (checking) androidx.compose.material3.CircularProgressIndicator(
                            Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary
                        ) else Text("Сохранить и проверить")
                    }
                }
                keyCheck?.let { (ok, text) ->
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (ok) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline, null,
                            tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text, style = MaterialTheme.typography.bodyMedium,
                            color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun AccentDot(accent: AccentColor, selected: Boolean, onClick: () -> Unit) {
    val color = if (isDarkSurface()) accent.swatch.darkPrimary else accent.swatch.lightPrimary
    val ringWidth by animateDpAsState(if (selected) 3.dp else 0.dp, spring(dampingRatio = 0.5f), label = "ring")
    val ringColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, label = "ringColor"
    )
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(ringWidth, ringColor, CircleShape)
            .padding(5.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(selected, enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn(), exit = scaleOut() + fadeOut()) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = if (isDarkSurface()) Color.Black else Color.White
            )
        }
    }
}
