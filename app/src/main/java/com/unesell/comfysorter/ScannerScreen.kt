package com.unesell.comfysorter

import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.*
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(onQrScanned: (String) -> Unit, onBack: () -> Unit) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    if (cameraPermissionState.status.isGranted) {
        CameraPreviewWithOverlay(onQrScanned, onBack)
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1D1B20)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Доступ к камере", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) { Text("Разрешить") }
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreviewWithOverlay(onQrScanned: (String) -> Unit, onBack: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isScanned by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Камера
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

                    val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
                    val imageAnalyzer = ImageAnalysis.Builder().setTargetResolution(Size(1280, 720)).build()

                    imageAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null && !isScanned) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            scanner.process(image).addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    barcode.rawValue?.let { url ->
                                        if (!isScanned && (url.startsWith("http") || url.contains("ngrok"))) {
                                            isScanned = true
                                            onQrScanned(url)
                                        }
                                    }
                                }
                            }.addOnCompleteListener { imageProxy.close() }
                        } else { imageProxy.close() }
                    }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
                    } catch (e: Exception) {}
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Затемнение и красивый вырез прицела
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cutoutWidth = 300.dp.toPx()
            val cutoutHeight = 300.dp.toPx()
            val rect = Rect(
                center = Offset(size.width / 2, size.height / 2 - 60.dp.toPx()),
                radius = cutoutWidth / 2
            )
            val cornerRadius = CornerRadius(40.dp.toPx(), 40.dp.toPx())

            // Темный фон
            drawRect(color = Color(0xFF1D1B20).copy(alpha = 0.85f))

            // Вырез окна (прозрачность)
            drawRoundRect(
                color = Color.Transparent,
                topLeft = rect.topLeft, size = rect.size,
                cornerRadius = cornerRadius, blendMode = BlendMode.Clear
            )

            // Рамки (уголки прицела)
            val cornerLength = 40.dp.toPx()
            val strokeW = 6.dp.toPx()
            val path = Path().apply {
                // Левый верхний
                moveTo(rect.left, rect.top + cornerLength)
                lineTo(rect.left, rect.top + cornerRadius.x)
                quadraticBezierTo(rect.left, rect.top, rect.left + cornerRadius.x, rect.top)
                lineTo(rect.left + cornerLength, rect.top)
                // Правый верхний
                moveTo(rect.right - cornerLength, rect.top)
                lineTo(rect.right - cornerRadius.x, rect.top)
                quadraticBezierTo(rect.right, rect.top, rect.right, rect.top + cornerRadius.y)
                lineTo(rect.right, rect.top + cornerLength)
                // Левый нижний
                moveTo(rect.left, rect.bottom - cornerLength)
                lineTo(rect.left, rect.bottom - cornerRadius.y)
                quadraticBezierTo(rect.left, rect.bottom, rect.left + cornerRadius.x, rect.bottom)
                lineTo(rect.left + cornerLength, rect.bottom)
                // Правый нижний
                moveTo(rect.right - cornerLength, rect.bottom)
                lineTo(rect.right - cornerRadius.x, rect.bottom)
                quadraticBezierTo(rect.right, rect.bottom, rect.right, rect.bottom - cornerRadius.y)
                lineTo(rect.right, rect.bottom - cornerLength)
            }
            drawPath(path, color = Color.White, style = Stroke(width = strokeW))
        }

        // Кнопка назад
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(top = 48.dp, start = 24.dp).size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))
        ) { Icon(Icons.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White) }

        // Нижняя стеклянная плашка
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF28242E).copy(alpha = 0.8f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Сканирование сервера", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Наведите камеру на QR-код\nв интерфейсе Comfy File Sorter", color = Color(0xFF9A9A9A), fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        }
    }
}