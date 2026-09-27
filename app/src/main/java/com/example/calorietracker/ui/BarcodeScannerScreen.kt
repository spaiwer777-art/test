package com.example.calorietracker.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.data.Food
import com.example.calorietracker.data.MealType
import com.example.calorietracker.viewmodel.DiaryViewModel
import com.example.calorietracker.viewmodel.ScanState
import com.example.calorietracker.viewmodel.ScannerViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(onDone: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val vm: ScannerViewModel = viewModel()
    val diaryVm: DiaryViewModel = viewModel()
    val state by vm.state.collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Сканировать штрихкод") }) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (hasPermission) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            val scanner = BarcodeScanning.getClient()
                            analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    scanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            barcodes.firstOrNull()?.rawValue?.let { vm.onBarcodeDetected(it) }
                                        }
                                        .addOnCompleteListener { imageProxy.close() }
                                } else {
                                    imageProxy.close()
                                }
                            }
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    analysis
                                )
                            } catch (_: Exception) {
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нужен доступ к камере для сканирования штрихкодов")
                }
            }

            when (val s = state) {
                is ScanState.Loading -> BottomBanner { Text("Ищу продукт...") }
                is ScanState.NotFound -> BottomBanner {
                    Text("Продукт не найден в базе. Добавь его вручную.")
                    TextButton(onClick = { vm.reset(); onBack() }) { Text("Ок") }
                }
                is ScanState.Error -> BottomBanner {
                    Text("Ошибка: ${s.message}")
                    TextButton(onClick = { vm.reset() }) { Text("Повторить") }
                }
                is ScanState.Found -> {
                    FoundFoodDialog(
                        food = s.food,
                        onDismiss = { vm.reset() },
                        onConfirm = { grams, meal ->
                            diaryVm.addEntry(
                                foodName = s.food.name,
                                grams = grams,
                                calsPer100 = s.food.caloriesPer100g,
                                proteinPer100 = s.food.proteinPer100g,
                                fatPer100 = s.food.fatPer100g,
                                carbsPer100 = s.food.carbsPer100g,
                                meal = meal
                            )
                            vm.reset()
                            onDone()
                        }
                    )
                }
                ScanState.Idle -> Unit
            }
        }
    }
}

@Composable
private fun BottomBanner(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun FoundFoodDialog(
    food: Food,
    onDismiss: () -> Unit,
    onConfirm: (grams: Double, meal: MealType) -> Unit
) {
    var grams by remember { mutableStateOf("100") }
    var meal by remember { mutableStateOf(MealType.BREAKFAST) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(food.name) },
        text = {
            Column {
                Text("${food.caloriesPer100g.toInt()} ккал / 100 г")
                OutlinedTextField(value = grams, onValueChange = { grams = it }, label = { Text("Граммы") })
                Box {
                    TextButton(onClick = { expanded = true }) { Text("Приём пищи: ${meal.label()}") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        MealType.entries.forEach {
                            DropdownMenuItem(text = { Text(it.label()) }, onClick = { meal = it; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(grams.toDoubleOrNull() ?: 100.0, meal) }) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
