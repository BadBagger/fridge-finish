package com.fridgefinish.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.fridgefinish.app.data.BarcodeProduct
import coil.compose.AsyncImage
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerScreen(
    lookupState: BarcodeLookupState,
    onBarcodeScanned: (String) -> Unit,
    onUseProduct: (BarcodeProduct) -> Unit,
    onUseBarcodeManually: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    var lastDetectedBarcode by remember { mutableStateOf("") }
    var manualBarcode by remember { mutableStateOf("") }
    var showCamera by remember { mutableStateOf(true) }
    var showManualFallback by remember { mutableStateOf(false) }
    val productFound = lookupState is BarcodeLookupState.Found
    val scannerActive = lookupState !is BarcodeLookupState.Loading && lookupState !is BarcodeLookupState.Found

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(productFound) {
        if (productFound) {
            showCamera = false
            showManualFallback = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScanHeroCard(lookupState)

        if (hasPermission && showCamera) {
            BarcodeCameraPreview { barcode ->
                if (scannerActive) {
                    lastDetectedBarcode = barcode
                    manualBarcode = barcode
                    onBarcodeScanned(barcode)
                }
            }
        } else if (!hasPermission) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Camera permission is needed to scan barcodes.")
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Allow camera")
                    }
                }
            }
        } else if (productFound) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Column(Modifier.weight(1f)) {
                        Text("Camera paused", style = MaterialTheme.typography.titleMedium)
                        Text("Review the product below or scan another barcode.")
                    }
                    OutlinedButton(onClick = { showCamera = true }) {
                        Text("Rescan")
                    }
                }
            }
        }

        if (lastDetectedBarcode.isNotBlank()) {
            AssistChip(onClick = {}, label = { Text("Detected $lastDetectedBarcode") })
        }

        when (lookupState) {
            BarcodeLookupState.Idle -> ScanTipCard()
            is BarcodeLookupState.Loading -> {
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Text("Looking up ${lookupState.barcode}...")
                    }
                }
            }
            is BarcodeLookupState.Found -> {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Product found", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            lookupState.product.imageUrl?.takeIf { it.isNotBlank() }?.let {
                                AsyncImage(
                                    model = it,
                                    contentDescription = lookupState.product.name,
                                    modifier = Modifier.size(76.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(lookupState.product.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("Category guess: ${lookupState.product.category.label}")
                                Text("Barcode: ${lookupState.product.barcode}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text("Next: check the printed date, then save the item.", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { onUseProduct(lookupState.product) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Review and add")
                        }
                        OutlinedButton(onClick = { showManualFallback = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Enter barcode instead")
                        }
                    }
                }
            }
            is BarcodeLookupState.NotFound -> {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No product match found.")
                        Text("You can still add it manually with this barcode saved.")
                        Button(onClick = { onUseBarcodeManually(lookupState.barcode) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Add manually")
                        }
                    }
                }
            }
        }

        if (!productFound || showManualFallback) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Manual fallback", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = manualBarcode,
                        onValueChange = { manualBarcode = it.filter(Char::isDigit) },
                        label = { Text("Barcode number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = {
                            if (manualBarcode.isNotBlank()) {
                                showCamera = false
                                onBarcodeScanned(manualBarcode)
                            }
                        }, modifier = Modifier.weight(1f)) {
                            Text("Look up")
                        }
                        OutlinedButton(onClick = { if (manualBarcode.isNotBlank()) onUseBarcodeManually(manualBarcode) }, modifier = Modifier.weight(1f)) {
                            Text("Add")
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

@Composable
private fun ScanHeroCard(lookupState: BarcodeLookupState) {
    val title = when (lookupState) {
        BarcodeLookupState.Idle -> "Find product fast"
        is BarcodeLookupState.Loading -> "Looking up barcode"
        is BarcodeLookupState.Found -> "Ready to review"
        is BarcodeLookupState.NotFound -> "No match found"
    }
    val body = when (lookupState) {
        BarcodeLookupState.Idle -> "Center the UPC/EAN barcode to fill product details."
        is BarcodeLookupState.Loading -> "Hold steady while Fridge Finish checks the product database."
        is BarcodeLookupState.Found -> "Product details are filled. You still choose the date before saving."
        is BarcodeLookupState.NotFound -> "Use the barcode anyway and add the product name yourself."
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(body, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun ScanTipCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null)
            Text("Barcode scanning identifies the product, not the expiration date. Scan the date on the food form after review.")
        }
    }
}

@Composable
private fun BarcodeCameraPreview(onBarcodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var lastBarcode by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    AndroidView(
        modifier = Modifier.fillMaxWidth().height(280.dp),
        factory = { viewContext ->
            val previewView = PreviewView(viewContext).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val providerFuture = ProcessCameraProvider.getInstance(viewContext)
            providerFuture.addListener(
                {
                    val cameraProvider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor) { imageProxy ->
                                scanImageProxy(imageProxy) { barcode ->
                                    if (barcode != lastBarcode) {
                                        lastBarcode = barcode
                                        onBarcodeScanned(barcode)
                                    }
                                }
                            }
                        }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analyzer
                    )
                },
                ContextCompat.getMainExecutor(context)
            )
            previewView
        }
    )
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun scanImageProxy(imageProxy: ImageProxy, onBarcode: (String) -> Unit) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E
        )
        .build()
    BarcodeScanning.getClient(options)
        .process(image)
        .addOnSuccessListener { barcodes ->
            barcodes.firstOrNull()?.rawValue?.takeIf { it.isNotBlank() }?.let(onBarcode)
        }
        .addOnCompleteListener { imageProxy.close() }
}
