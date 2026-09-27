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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorietracker.viewmodel.ScanState
import com.example.calorietracker.viewmodel.ScannerViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun BarcodeScannerScreen(onFound: (Long) -> Unit, onSearchByName: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val vm: ScannerViewModel = viewModel()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сканер штрихкода") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
                }
            )
        }
    ) { padding ->
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
                ScannerOverlay()
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нужен доступ к камере для сканирования штрихкодов")
                }
            }

            when (val s = state) {
                is ScanState.Loading -> BottomBanner { Text("Ищу продукт...") }
                is ScanState.NotFound -> BottomBanner {
                    Text("Этого штрихкода нет в Open Food Facts.", style = MaterialTheme.typography.titleSmall)
                    Text("Найди продукт по названию — в справочной базе, онлайн или посчитай с ИИ.")
                    Row {
                        TextButton(onClick = { vm.reset(); onSearchByName() }) { Text("Искать по названию") }
                        TextButton(onClick = { vm.reset() }) { Text("Сканировать ещё") }
                    }
                }
                is ScanState.Error -> BottomBanner {
                    Text("Ошибка: ${s.message}")
                    TextButton(onClick = { vm.reset() }) { Text("Повторить") }
                }
                is ScanState.Found -> LaunchedEffect(s.food.id) {
                    vm.reset()
                    onFound(s.food.id)
                }
                ScanState.Idle -> Unit
            }
        }
    }
}

@Composable
private fun BoxScope.BottomBanner(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

/** Rounded viewfinder with a sweeping scan line. */
@Composable
private fun BoxScope.ScannerOverlay() {
    val transition = rememberInfiniteTransition(label = "scan")
    val lineFraction by transition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "line"
    )
    val accent = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .align(Alignment.Center)
            .fillMaxWidth(0.8f)
            .aspectRatio(1.6f)
            .border(3.dp, Color.White.copy(alpha = 0.9f), MaterialTheme.shapes.medium)
            .drawBehind {
                val y = size.height * lineFraction
                drawLine(accent, Offset(16.dp.toPx(), y), Offset(size.width - 16.dp.toPx(), y), 3.dp.toPx())
            }
    )
    Text(
        "Наведи камеру на штрихкод",
        color = Color.White,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp)
    )
}
