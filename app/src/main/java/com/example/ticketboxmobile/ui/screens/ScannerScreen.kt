package com.example.ticketboxmobile.ui.screens

import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ticketboxmobile.p2p.P2PManager
import com.example.ticketboxmobile.ui.theme.scan_valid_bg
import com.example.ticketboxmobile.ui.theme.scan_invalid_bg
import com.example.ticketboxmobile.ui.theme.scan_used_bg
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
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
            title = { Text("Xác nhận thoát", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn sắp thoát khỏi chế độ Máy Quét. Toàn bộ nhật ký (logs) sẽ bị xoá.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        p2pManager.clearLogs()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Thoát")
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
            kotlinx.coroutines.delay(2000)
            p2pManager.clearValidationResult()
            scannedCode = null
            isScanningEnabled = true
        }
    }

    // Effect to handle timeout if Hub doesn't respond or is disconnected
    LaunchedEffect(scannedCode, validationResult) {
        if (scannedCode != null && validationResult == null) {
            kotlinx.coroutines.delay(3000) // 3 seconds timeout
            if (validationResult == null) {
                p2pManager.addLog("Quá thời gian chờ phản hồi từ Hub")
                scannedCode = null
                isScanningEnabled = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview Layer
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
                            val options = com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS)
                                .build()
                            val scanner = BarcodeScanning.getClient(options)
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        val qrValue = barcode.rawValue
                                        if (qrValue != null && isScanningEnabled) {
                                            isScanningEnabled = false
                                            scannedCode = qrValue
                                            p2pManager.sendQrHashToHub(qrValue)
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

        // Overlay targeting reticle (optional, visual enhancement)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(250.dp)
                .background(Color.Transparent)
        )

        // Top Bar overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = { showExitDialog = true },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Đóng", tint = MaterialTheme.colorScheme.onSurface)
            }
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "MÁY QUÉT (SCANNER)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Result & Logs Bottom Sheet Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Validation Result Animated Area
            AnimatedVisibility(
                visible = validationResult != null || scannedCode != null,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val color = when (validationResult) {
                    "VALID" -> scan_valid_bg
                    "VIP_GUEST" -> Color(0xFFFFD700)
                    "USED" -> scan_used_bg
                    "INVALID" -> scan_invalid_bg
                    "DISCONNECTED" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }
                
                val icon = when (validationResult) {
                    "VALID" -> Icons.Default.CheckCircle
                    "VIP_GUEST" -> Icons.Default.Star
                    "USED" -> Icons.Default.Warning
                    "INVALID" -> Icons.Default.Clear
                    "DISCONNECTED" -> Icons.Default.Warning
                    else -> null
                }
                
                val textMsg = when (validationResult) {
                    "VALID" -> "VÉ HỢP LỆ"
                    "VIP_GUEST" -> "VÉ KHÁCH MỜI VIP"
                    "USED" -> "VÉ ĐÃ SỬ DỤNG!"
                    "INVALID" -> "VÉ KHÔNG TỒN TẠI"
                    "DISCONNECTED" -> "CHƯA KẾT NỐI HUB"
                    else -> "ĐANG XÁC THỰC..."
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(64.dp).padding(bottom = 8.dp)
                            )
                        } else {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.padding(bottom = 16.dp))
                        }
                        
                        Text(
                            text = textMsg,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (scannedCode != null && validationResult != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Mã: $scannedCode",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Logs Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp, max = 250.dp)
                    .padding(24.dp)
            ) {
                Text(
                    text = "Lịch sử kết nối",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}
