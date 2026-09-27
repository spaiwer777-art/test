package com.example.calorietracker.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.calorietracker.R
import com.example.calorietracker.viewmodel.AccountStatus

/** What the first screen shows once the user is signed in. */
internal data class SignedIn(val email: String, val hasCloudData: Boolean?)

/**
 * First screen of the app: brand hero plus sign-up / sign-in, shown before the profile questions.
 * Stateless so it can be screenshot-tested; [OnboardingScreen] wires it to [AccountViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AuthContent(
    cloudConfigured: Boolean,
    googleEnabled: Boolean,
    status: AccountStatus,
    signedIn: SignedIn?,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onGoogle: () -> Unit,
    onDownload: () -> Unit,
    onContinue: () -> Unit,
    onRestoreFile: () -> Unit,
    initialLogin: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    val bg = Brush.verticalGradient(
        0f to scheme.primaryContainer,
        0.45f to scheme.primaryContainer.copy(alpha = 0.35f),
        1f to scheme.background
    )
    Box(Modifier.fillMaxSize().background(scheme.background).background(bg)) {
        // Soft decorative circles behind the logo.
        Box(Modifier.padding(start = 220.dp, top = 30.dp).size(200.dp).clip(CircleShape).background(scheme.primary.copy(alpha = 0.08f)))
        Box(Modifier.padding(start = 0.dp, top = 170.dp).size(120.dp).clip(CircleShape).background(scheme.tertiary.copy(alpha = 0.08f)))

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Image(
                painterResource(R.drawable.logo), contentDescription = null,
                modifier = Modifier.size(116.dp).shadow(12.dp, RoundedCornerShape(32.dp)).clip(RoundedCornerShape(32.dp))
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "Калории под контролем",
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Создай аккаунт — дневник, рецепты и вес сохранятся в облаке и переедут на новый телефон.",
                style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Perk(Icons.Filled.QrCodeScanner, "Сканер")
                Perk(Icons.Filled.AutoAwesome, "ИИ-подсчёт")
                Perk(Icons.Filled.MenuBook, "Рецепты")
            }
            Spacer(Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    when {
                        !cloudConfigured -> NoCloud(onContinue)
                        signedIn != null -> SignedInPanel(signedIn, status, onDownload, onContinue)
                        else -> AuthForm(googleEnabled, status, onSignIn, onSignUp, onGoogle, initialLogin)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            if (signedIn == null && cloudConfigured) {
                TextButton(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("Продолжить без аккаунта", style = MaterialTheme.typography.titleSmall)
                }
            }
            TextButton(onClick = onRestoreFile) {
                Text("Восстановить из файла резервной копии", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Perk(icon: ImageVector, text: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthForm(
    googleEnabled: Boolean,
    status: AccountStatus,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onGoogle: () -> Unit,
    initialLogin: Boolean
) {
    var login by rememberSaveable { mutableStateOf(initialLogin) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val busy = status is AccountStatus.Busy
    // After sign-up with email confirmation, switch to the login tab so the next tap is «Войти».
    val needsConfirm = status is AccountStatus.Done && status.text.contains("Подтверди")
    LaunchedEffect(needsConfirm) { if (needsConfirm) login = true }

    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        SegmentedButton(selected = !login, onClick = { login = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Регистрация") }
        SegmentedButton(selected = login, onClick = { login = true }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Вход") }
    }
    Spacer(Modifier.height(16.dp))

    if (googleEnabled) {
        OutlinedButton(
            onClick = onGoogle, enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text("G", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
            Spacer(Modifier.width(12.dp))
            Text("Продолжить с Google", color = MaterialTheme.colorScheme.onSurface)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f))
            Text("  или по почте  ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.weight(1f))
        }
    }

    val fieldShape = RoundedCornerShape(16.dp)
    OutlinedTextField(
        email, { email = it.trim() },
        label = { Text("Email") },
        leadingIcon = { Icon(Icons.Filled.Email, null) },
        singleLine = true, shape = fieldShape,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        password, { password = it },
        label = { Text("Пароль") },
        supportingText = if (!login) ({ Text("Не меньше 6 символов") }) else null,
        leadingIcon = { Icon(Icons.Filled.Lock, null) },
        trailingIcon = {
            IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                    if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    if (showPassword) "Скрыть пароль" else "Показать пароль"
                )
            }
        },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true, shape = fieldShape,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(14.dp))

    StatusLine(status)

    val valid = email.contains('@') && email.contains('.') && password.length >= 6 && !busy
    Button(
        onClick = { if (login) onSignIn(email, password) else onSignUp(email, password) },
        enabled = valid,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(54.dp)
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(if (login) "Войти" else "Создать аккаунт", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun StatusLine(status: AccountStatus) {
    AnimatedVisibility(status is AccountStatus.Done || status is AccountStatus.Failed) {
        val failed = status is AccountStatus.Failed
        val text = (status as? AccountStatus.Done)?.text ?: (status as? AccountStatus.Failed)?.text ?: ""
        val color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (failed) Icons.Filled.ErrorOutline else Icons.Filled.MarkEmailRead, null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun SignedInPanel(user: SignedIn, status: AccountStatus, onDownload: () -> Unit, onContinue: () -> Unit) {
    val busy = status is AccountStatus.Busy
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.AccountCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Ты вошёл", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(user.email, style = MaterialTheme.typography.titleMedium)
        }
    }
    Spacer(Modifier.height(16.dp))
    if (status is AccountStatus.Failed) StatusLine(status)
    AnimatedContent(user.hasCloudData, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "cloud") { has ->
        Column {
            when (has) {
                null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Проверяю облако…", style = MaterialTheme.typography.bodyMedium)
                }
                true -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CloudDone, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("В облаке есть твои данные", style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = onDownload, enabled = !busy, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(54.dp)) {
                        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else {
                            Icon(Icons.Filled.CloudDownload, null); Spacer(Modifier.width(8.dp)); Text("Загрузить мои данные")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onContinue, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Начать заново (облако перезапишется)")
                    }
                }
                false -> {
                    Text(
                        "Осталось ответить на пару вопросов — рассчитаем норму калорий и БЖУ.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = onContinue, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(54.dp)) {
                        Text("Далее", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun NoCloud(onContinue: () -> Unit) {
    Text("Ответь на пару вопросов — и мы рассчитаем твою норму калорий и БЖУ.", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(14.dp))
    Button(onClick = onContinue, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(54.dp)) {
        Text("Начать", style = MaterialTheme.typography.titleMedium)
    }
}
