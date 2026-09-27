package com.example.calorietracker.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.calorietracker.AppIcon
import com.example.calorietracker.LauncherIcons
import com.example.calorietracker.ui.components.AppTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Settings block «Значок и название»: wires [AppIconContent] to the launcher and shortcut APIs. */
@Composable
fun AppIconSection(appName: String, onAppName: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf(LauncherIcons.current(context)) }
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    // Typed locally (instant), saved on every change; picks up the stored name once it loads.
    var name by remember { mutableStateOf(appName) }
    androidx.compose.runtime.LaunchedEffect(appName) { if (name.isEmpty()) name = appName }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            photo = withContext(Dispatchers.IO) { runCatching { LauncherIcons.loadSquare(context, uri) }.getOrNull() }
            if (photo == null) message = "Не удалось открыть картинку"
        }
    }
    AppIconContent(
        current = current,
        onPick = { icon ->
            LauncherIcons.apply(context, icon)
            current = icon
            message = "Значок в меню приложений сменится через несколько секунд. " +
                "Если он пропал с рабочего стола — перетащи его заново из меню."
        },
        appName = name,
        onAppName = { name = it; onAppName(it) },
        photo = photo?.asImageBitmap(),
        onPickPhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onClearPhoto = { photo = null },
        canPin = LauncherIcons.canPinShortcut(context),
        onPin = {
            val ok = LauncherIcons.pinShortcut(context, name, current, photo)
            message = if (ok) "Подтверди добавление в окне телефона — значок появится на рабочем столе."
            else "Этот рабочий стол не поддерживает добавление значков из приложения."
        },
        message = message
    )
}

@Composable
internal fun AppIconContent(
    current: AppIcon,
    onPick: (AppIcon) -> Unit,
    appName: String,
    onAppName: (String) -> Unit,
    photo: ImageBitmap?,
    onPickPhoto: () -> Unit,
    onClearPhoto: () -> Unit,
    canPin: Boolean,
    onPin: () -> Unit,
    message: String?
) {
    Text("Значок приложения", style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        AppIcon.entries.forEach { icon -> IconChoice(icon, selected = icon == current) { if (icon != current) onPick(icon) } }
    }
    message?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }

    Spacer(Modifier.height(20.dp))
    Text("Свой значок на рабочем столе", style = MaterialTheme.typography.labelLarge)
    Text(
        "Android не даёт приложению переименовать себя, поэтому своё название и картинку ставим " +
            "ярлыком на рабочий стол — он открывает приложение так же, как обычный значок.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.TopEnd) {
            val shape = RoundedCornerShape(20.dp)
            if (photo != null) {
                Image(photo, null, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(shape))
            } else {
                IconImage(current, Modifier.size(64.dp).clip(shape))
            }
            if (photo != null) {
                Box(
                    Modifier.size(22.dp).clip(CircleShape).clickable(onClick = onClearPhoto),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(20.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, "Убрать картинку", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        AppTextField(
            value = appName,
            onValueChange = { onAppName(it.take(25)) },
            label = { Text("Название") },
            placeholder = { Text("CalorieTracker") },
            leadingIcon = { Icon(Icons.Filled.Edit, null) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onPickPhoto, modifier = Modifier.weight(1f).height(48.dp)) {
            Icon(Icons.Filled.AddPhotoAlternate, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Картинка", maxLines = 1)
        }
        Button(onClick = onPin, enabled = canPin, modifier = Modifier.weight(1f).height(48.dp)) {
            Icon(Icons.Filled.AddToHomeScreen, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("На экран", maxLines = 1)
        }
    }
}

@Composable
private fun IconChoice(icon: AppIcon, selected: Boolean, onClick: () -> Unit) {
    val ring by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "ring")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp)) {
        Box(
            Modifier.size(68.dp).clip(RoundedCornerShape(22.dp)).border(BorderStroke(3.dp, ring), RoundedCornerShape(22.dp))
                .clickable(onClick = onClick).padding(5.dp),
            contentAlignment = Alignment.Center
        ) { IconImage(icon, Modifier.size(58.dp).clip(RoundedCornerShape(18.dp))) }
        Spacer(Modifier.height(4.dp))
        Text(
            icon.label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Draws the icon's two adaptive layers the way a launcher shows them (outer third is masked off). */
@Composable
private fun IconImage(icon: AppIcon, modifier: Modifier) {
    Box(modifier.background(Color(icon.background)), contentAlignment = Alignment.Center) {
        Image(
            androidx.compose.ui.res.painterResource(icon.foreground), icon.label,
            modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = 1.5f, scaleY = 1.5f)
        )
    }
}
