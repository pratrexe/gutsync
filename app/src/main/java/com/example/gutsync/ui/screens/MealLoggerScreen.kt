package com.example.gutsync.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gutsync.GutSyncViewModel
import com.example.gutsync.UiState
import com.example.gutsync.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MealLoggerScreen(viewModel: GutSyncViewModel = viewModel()) {
    var showManualDialog by remember { mutableStateOf(false) }
    var mealToDelete by remember { mutableStateOf<com.example.gutsync.data.storage.MealLogEntry?>(null) }
    val appData by viewModel.appData.collectAsState()
    val analyzedFood by viewModel.analyzedFood.collectAsState()
    val openRouterExplanation by viewModel.openRouterExplanation.collectAsState()
    val uiState by viewModel.analysisState.collectAsState()
    val capturedImage by viewModel.capturedImage.collectAsState()
    val identifiedFoodName by viewModel.identifiedFoodName.collectAsState()
    var quantityInput by remember { mutableStateOf("100") }
    val context = LocalContext.current

    val photoUri = remember {
        val imagesDir = File(context.cacheDir, "images")
        if (!imagesDir.exists()) imagesDir.mkdirs()
        val tempFile = File.createTempFile("captured_meal_", ".jpg", imagesDir)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            try {
                context.contentResolver.openInputStream(photoUri)?.use { stream ->
                    val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                    viewModel.setCapturedImage(bitmap)
                    viewModel.identifyFoodFromPhoto(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(photoUri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Food Logic",
            style = Typography.headlineMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Scanner Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(24.dp))
                    .clickable {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            cameraLauncher.launch(photoUri)
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (capturedImage != null) {
                    Image(
                        bitmap = capturedImage!!.asImageBitmap(),
                        contentDescription = "Captured Meal",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                    Icon(Icons.Default.CameraAlt, "Retake", tint = Color.White)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Snap Meal", color = Color.White, fontSize = 14.sp)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(24.dp))
                    .clickable {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                             val options = com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder()
                                .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS)
                                .build()
                            val scanner = com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context, options)
                            scanner.startScan()
                                .addOnSuccessListener { barcode ->
                                    barcode.rawValue?.let { viewModel.analyzeBarcode(it) }
                                }
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.QrCodeScanner, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Scan Barcode", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Analysis Result Section
        AnimatedVisibility(
            visible = uiState !is UiState.Initial || identifiedFoodName != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = SurfaceContainerHigh,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    when (uiState) {
                        is UiState.Loading -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Analyzing microbiome impact...", color = OnSurfaceVariant, fontSize = 14.sp)
                            }
                        }
                        is UiState.Error -> {
                            Text("Error: ${(uiState as UiState.Error).errorMessage}", color = Color(0xFFFFB4AB))
                        }
                        else -> {
                            if (identifiedFoodName != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Identified:", color = OnSurfaceVariant, fontSize = 12.sp)
                                        Text(identifiedFoodName!!, color = White, style = Typography.titleLarge)
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = quantityInput,
                                            onValueChange = { quantityInput = it },
                                            modifier = Modifier.width(80.dp),
                                            label = { Text("Grams", fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = White,
                                                unfocusedTextColor = White
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { 
                                                val grams = quantityInput.toFloatOrNull() ?: 100f
                                                viewModel.analyzeFood(identifiedFoodName!!, capturedImage, grams) 
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Black)
                                        ) {
                                            Text("Analyze")
                                        }
                                    }
                                }
                            }

                            analyzedFood?.let { food ->
                                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Outline)
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(food.foodName, style = Typography.titleMedium, color = White)
                                    Surface(
                                        color = Color(0xFF2C2C2E),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            "${food.calories} kcal",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            color = White,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                
                                openRouterExplanation?.let { exp ->
                                    Text(
                                        text = exp,
                                        color = OnSurfaceVariant,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Button(
                                    onClick = { viewModel.addAnalyzedFood() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = White)
                                ) {
                                    Text("Log to Gut Graph", color = Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // History Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent Meals", style = Typography.titleMedium, color = White)
            IconButton(onClick = { showManualDialog = true }) {
                Icon(Icons.Default.Add, "Manual Entry", tint = White)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(appData.meals.reversed()) { meal ->
                MealCard(
                    meal = meal, 
                    onDelete = { mealToDelete = meal }
                )
            }
        }
    }

    // Delete Confirmation
    if (mealToDelete != null) {
        AlertDialog(
            onDismissRequest = { mealToDelete = null },
            title = { Text("Delete Meal Entry?") },
            text = { Text("This will remove the data from your gut health history.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMeal(mealToDelete!!)
                    mealToDelete = null
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { mealToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showManualDialog) {
        ManualFoodDialog(
            onDismiss = { showManualDialog = false },
            onLog = { name, fiber, starch, poly ->
                val nutrients = com.example.gutsync.data.NutrientData(
                    foodName = name,
                    fiber = fiber,
                    resistantStarch = starch,
                    polyphenols = poly
                )
                viewModel.logManualMeal(nutrients)
                showManualDialog = false
            }
        )
    }
}

@Composable
fun MealCard(meal: com.example.gutsync.data.storage.MealLogEntry, onDelete: () -> Unit) {
    Surface(
        color = SurfaceContainerHigh,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Meal Image
            if (meal.imageBase64 != null) {
                val bitmap = remember(meal.imageBase64) {
                    val bytes = android.util.Base64.decode(meal.imageBase64, android.util.Base64.DEFAULT)
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Restaurant, null, tint = OnSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(meal.nutrients.foodName, color = White, fontWeight = FontWeight.Bold)
                Text(
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(meal.timestamp)),
                    color = OnSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("+${meal.nutrients.fiber}g fiber", color = Color(0xFF4ADE80), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = OnSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun ManualFoodDialog(onDismiss: () -> Unit, onLog: (String, Float, Float, Float) -> Unit) {
    var name by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    var starch by remember { mutableStateOf("") }
    var polyphenols by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Food Manually") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Food Name") })
                OutlinedTextField(value = fiber, onValueChange = { fiber = it }, label = { Text("Fiber (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = starch, onValueChange = { starch = it }, label = { Text("Resistant Starch (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = polyphenols, onValueChange = { polyphenols = it }, label = { Text("Polyphenols (mg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            Button(onClick = {
                onLog(name, fiber.toFloatOrNull() ?: 0f, starch.toFloatOrNull() ?: 0f, polyphenols.toFloatOrNull() ?: 0f)
            }) { Text("Log") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
