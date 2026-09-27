package com.example.calorietracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.ui.components.IconBadge
import com.example.calorietracker.ui.components.SectionCard
import com.example.calorietracker.viewmodel.AccountStatus
import com.example.calorietracker.viewmodel.AccountViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val syncFormatter = DateTimeFormatter.ofPattern("d MMMM, HH:mm", Locale("ru"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(onBack: () -> Unit) {
    val vm: AccountViewModel = viewModel()
    val session by vm.session.collectAsState()
    val lastSync by vm.lastSync.collectAsState()
    val autoSync by vm.autoSync.collectAsState()
    val status by vm.status.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var includePhotos by remember { mutableStateOf(true) }
    val busy = status is AccountStatus.Busy
    val context = androidx.compose.ui.platform.LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { vm.exportTo(it, includePhotos) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { confirm = "Данные на этом телефоне будут заменены данными из файла." to { vm.importFrom(it) } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Аккаунт и резервная копия") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedContent(status, label = "status", contentKey = { it::class }) { s ->
                when (s) {
                    AccountStatus.Idle -> Spacer(Modifier.height(0.dp))
                    is AccountStatus.Busy -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(s.text)
                    }
                    is AccountStatus.Done -> Text("✓ ${s.text}", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
                    is AccountStatus.Failed -> Text(s.text, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
                }
            }

            val current = session
            if (!vm.cloudConfigured) {
                SectionCard(title = "Облачный аккаунт") {
                    Row {
                        Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Облако для этой сборки ещё не подключено. Пока переноси прогресс через файл резервной копии ниже — " +
                                "его можно сохранить в Google Диск, Telegram или на компьютер.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (current == null) {
                SectionCard(title = "Вход", subtitle = "Аккаунт хранит дневник, рецепты, диеты, вес и настройки в облаке") {
                    if (vm.googleEnabled) {
                        FilledTonalButton(onClick = { vm.signInWithGoogle(context) }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            Text("G", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text("Войти через Google")
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("или по почте", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                    }
                    AppTextField(
                        email, { email = it }, label = { Text("Email") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    AppTextField(
                        password, { password = it }, label = { Text("Пароль (от 6 символов)") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    val valid = email.contains('@') && password.length >= 6 && !busy
                    Button(onClick = { vm.signIn(email, password) }, enabled = valid, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Войти") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { vm.signUp(email, password) }, enabled = valid, modifier = Modifier.fillMaxWidth()) { Text("Создать аккаунт") }
                }
            } else {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.AccountCircle, MaterialTheme.colorScheme.primary, 52.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(current.email, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (lastSync > 0) "Сохранено: " + Instant.ofEpochMilli(lastSync).atZone(ZoneId.systemDefault()).format(syncFormatter)
                                else "Ещё не сохранялось в облако",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = vm::upload, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.CloudUpload, null); Spacer(Modifier.width(8.dp)); Text("Сохранить в облако")
                    }
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = { confirm = "Данные на этом телефоне будут заменены данными из облака." to { vm.download() } },
                        enabled = !busy, modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CloudDownload, null); Spacer(Modifier.width(8.dp)); Text("Загрузить из облака")
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Автосохранение", style = MaterialTheme.typography.bodyLarge)
                            Text("При выходе из приложения, не чаще раза в 15 минут", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = autoSync, onCheckedChange = vm::setAutoSync)
                    }
                    TextButton(onClick = vm::signOut, enabled = !busy) { Text("Выйти из аккаунта") }
                    Text(
                        "Фото приёмов пищи в облако не загружаются — их переносит файл резервной копии.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionCard(title = "Резервная копия в файл", subtitle = "Работает без интернета и аккаунта: zip-файл со всеми данными") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Включить фото", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = includePhotos, onCheckedChange = { includePhotos = it })
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { exportLauncher.launch("calorietracker-${LocalDate.now()}.zip") },
                    enabled = !busy, modifier = Modifier.fillMaxWidth()
                ) { Icon(Icons.Filled.FileDownload, null); Spacer(Modifier.width(8.dp)); Text("Сохранить копию") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                    enabled = !busy, modifier = Modifier.fillMaxWidth()
                ) { Icon(Icons.Filled.FileUpload, null); Spacer(Modifier.width(8.dp)); Text("Восстановить из файла") }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Перенос на новый телефон: сохрани копию → перешли себе файл → на новом телефоне «Восстановить из файла».",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    confirm?.let { (text, action) ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Заменить данные?") },
            text = { Text("$text Это нельзя отменить.") },
            confirmButton = { TextButton(onClick = { confirm = null; action() }) { Text("Заменить") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Отмена") } }
        )
    }
}
