package com.example.ticketboxmobile.ui.screens

import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ticketboxmobile.p2p.P2PManager
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    p2pManager: P2PManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val logs by p2pManager.logs.collectAsState()
    val validationResult by p2pManager.validationResult.collectAsState()

    var scannedCode by remember { mutableStateOf<String?>(null) }
    var isScanningEnabled by remember { mutableStateOf(true) }
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Xác nhận thoát") },
            text = { Text("Bạn sắp thoát khỏi chế độ Máy Quét. Toàn bộ nhật ký (logs) sẽ bị xoá.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    p2pManager.clearLogs()
                    onBack()
                }) {
                    Text("Đồng ý")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    DisposableEffect(Unit) {
        p2pManager.startDiscovery("Gate-Scanner-1")
        onDispose {
            p2pManager.stop()
        }
    }

    // Effect to handle validation result reset
    LaunchedEffect(validationResult) {
        if (validationResult != null) {
            // Show result temporarily, then allow next scan
            kotlinx.coroutines.delay(3000)
            p2pManager.clearValidationResult()
            scannedCode = null
            isScanningEnabled = true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val executor = Executors.newSingleThreadExecutor()
                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            if (!isScanningEnabled) {
                                imageProxy.close()
                                return@setAnalyzer
                            }

                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                val scanner = BarcodeScanning.getClient()
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            if (barcode.valueType == Barcode.TYPE_TEXT) {
                                                val qrValue = barcode.rawValue
                                                if (qrValue != null && isScanningEnabled) {
                                                    isScanningEnabled = false
                                                    scannedCode = qrValue
                                                    p2pManager.sendQrHashToHub(qrValue)
                                                }
                                            }
                                        }
                                    }
                                    .addOnFailureListener {
                                        Log.e("Scanner", "Error scanning: ${it.message}")
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (exc: Exception) {
                            Log.e("Scanner", "Use case binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Result Overlay
            if (validationResult != null) {
                val color = when (validationResult) {
                    "VALID" -> Color.Green
                    "USED" -> Color.Red
                    else -> Color.Gray
                }
                val textMsg = when (validationResult) {
                    "VALID" -> "VÉ HỢP LỆ"
                    "USED" -> "VÉ ĐÃ SỬ DỤNG!"
                    else -> "VÉ KHÔNG TỒN TẠI"
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = textMsg,
                            color = Color.White,
                            style = MaterialTheme.typography.displaySmall
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Mã QR: $scannedCode",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            } else if (scannedCode != null) {
                // Waiting for Hub result
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Đang chờ xác thực từ Máy trưởng...",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        // Bottom logs panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
        ) {
            Text("Logs P2P:", style = MaterialTheme.typography.titleSmall)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(logs) { log ->
                    Text(text = log, modifier = Modifier.padding(vertical = 2.dp), style = MaterialTheme.typography.bodySmall)
                    Divider()
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { showExitDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Quay lại")
            }
        }
    }
}
