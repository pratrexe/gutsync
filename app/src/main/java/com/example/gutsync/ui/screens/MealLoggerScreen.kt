package com.example.gutsync.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.gutsync.GutSyncViewModel
import com.example.gutsync.UiState
import com.example.gutsync.data.MicrobeImpactCalculator
import com.example.gutsync.data.NutrientData
import com.example.gutsync.ui.theme.SurfaceContainerLow
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

@Composable
fun MealLoggerScreen(viewModel: GutSyncViewModel = viewModel()) {
    var searchQuery by remember { mutableStateOf("") }
    var showManualDialog by remember { mutableStateOf(false) }
    val appData by viewModel.appData.collectAsState()
    val analyzedFood by viewModel.analyzedFood.collectAsState()
    val qwenExplanation by viewModel.qwenExplanation.collectAsState()
    val uiState by viewModel.analysisState.collectAsState()
    val capturedImage by viewModel.capturedImage.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, do nothing and wait for user to click again or auto-launch
        }
    }

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
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // 1. Search/Description Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "GUT INTELLIGENCE ENGINE (GIE)",
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search USDA/Open Food Facts...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceContainerLow,
                        unfocusedContainerColor = SurfaceContainerLow,
                        unfocusedBorderColor = Color(0xFF2C2C2E),
                        focusedBorderColor = Color.White
                    ),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                )
            }
        }

        // 2. Image Spotlight
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Surface(
                            modifier = Modifier.padding(16.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = CircleShape
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Retake",
                                tint = Color.White,
                                modifier = Modifier.padding(8.dp).size(20.dp)
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Capture Food",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Snap Photo for GIE Analysis",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // 3. Dynamic Action Button (Analyze or Confirm)
        item {
            if (uiState is UiState.Loading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                if (analyzedFood != null) {
                    Button(
                        onClick = { viewModel.addAnalyzedFood(); searchQuery = "" },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm and Log Meal", fontWeight = FontWeight.Bold)
                    }
                } else if (uiState is UiState.Error && capturedImage != null) {
                    // Improved Google Lens Fallback using standard SEND intent for local files
                    Button(
                        onClick = {
                            try {
                                val lensIntent = Intent("com.google.android.googlequicksearchbox.GOOGLE_LENS")
                                lensIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                lensIntent.setDataAndType(photoUri, "image/jpeg")
                                lensIntent.setPackage("com.google.android.googlequicksearchbox")
                                context.startActivity(lensIntent)
                            } catch (e: Exception) {
                                // Fallback to standard ACTION_SEND if custom action fails
                                try {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/jpeg"
                                        putExtra(Intent.EXTRA_STREAM, photoUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        setPackage("com.google.android.googlequicksearchbox")
                                    }
                                    context.startActivity(sendIntent)
                                } catch (e2: Exception) {
                                    // Final fallback to web browser
                                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/searchbyimage?image_url=$photoUri"))
                                    context.startActivity(webIntent)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4), contentColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Identify with Google Lens", fontWeight = FontWeight.Bold)
                    }
                } else if (capturedImage != null || searchQuery.isNotBlank()) {
                    Button(
                        onClick = { viewModel.analyzeFood(searchQuery, capturedImage) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze with Groq AI", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. Results (Only shown after analysis)
        if (analyzedFood != null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    GIEImpactCard(analyzedFood, modifier = Modifier.weight(1f))
                    GIEBioticDensityCard(analyzedFood, modifier = Modifier.weight(1f))
                }
            }

            item {
                val scorecard = MicrobeImpactCalculator.calculateGIE(analyzedFood!!)
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Data Source Badge
                    if (analyzedFood!!.sourceFound.isNotBlank()) {
                        Surface(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "Source: ${analyzedFood!!.sourceFound}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("GIE SCIENTIFIC INSIGHT", fontSize = 10.sp, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(qwenExplanation ?: scorecard.scientificReasoning, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                ScoreLabel("Inflammation Risk", "${scorecard.inflammationRisk}%")
                                ScoreLabel("Diversity Score", "${scorecard.diversityScore}%")
                            }
                        }
                    }
                }
            }
        }

        // 5. Recent Logs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Recent Logs", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                IconButton(onClick = { showManualDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Manual Log", tint = Color.White)
                }
            }
        }

        items(appData.meals.takeLast(10).reversed()) { entry ->
            RealRecentItemRow(entry)
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }

    if (showManualDialog) {
        ManualLogDialog(
            onDismiss = { showManualDialog = false },
            onConfirm = { nutrients, bitmap ->
                viewModel.logManualMeal(nutrients, bitmap)
                showManualDialog = false
            }
        )
    }
}

@Composable
fun ScoreLabel(label: String, value: String) {
    Column {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun GIEImpactCard(nutrients: NutrientData?, modifier: Modifier = Modifier) {
    val scorecard = nutrients?.let { MicrobeImpactCalculator.calculateGIE(it) }
    val score = scorecard?.gutHealthScore ?: 0
    val shiftText = "Predicting high confidence shifts in beneficial microbes."

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C2E)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.height(180.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "GUT HEALTH",
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = score.toString(), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "/100", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
            }
            Text(
                text = shiftText,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun GIEBioticDensityCard(nutrients: NutrientData?, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C2E)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.height(180.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "BIOTIC DENSITY",
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            BioticRow("Starch", "${nutrients?.resistantStarch ?: 0}g")
            BioticRow("Fiber", "${nutrients?.fiber ?: 0}g")
            BioticRow("Polyphenols", "${nutrients?.polyphenols ?: 0}mg")
        }
    }
}

@Composable
fun BioticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = Color.White)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun RealRecentItemRow(entry: com.example.gutsync.data.storage.MealLogEntry) {
    val nutrients = entry.nutrients
    val imageBitmap = remember(entry.imageBase64) {
        entry.imageBase64?.let { base64 ->
            try {
                val decodedString = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size).asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C2E)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photo Thumbnail
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Food Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = nutrients.foodName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(text = "Logged • ${nutrients.calories} kcal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Surface(
                color = Color.White.copy(alpha = 0.1f),
                shape = CircleShape
            ) {
                val score = MicrobeImpactCalculator.calculateGIE(nutrients).gutHealthScore
                Text(
                    text = score.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualLogDialog(
    onDismiss: () -> Unit,
    onConfirm: (NutrientData, Bitmap?) -> Unit
) {
    var foodName by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    var starch by remember { mutableStateOf("") }
    var sugar by remember { mutableStateOf("") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    val photoUri = remember {
        val imagesDir = File(context.cacheDir, "images")
        if (!imagesDir.exists()) imagesDir.mkdirs()
        val tempFile = File.createTempFile("manual_meal_", ".jpg", imagesDir)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            try {
                context.contentResolver.openInputStream(photoUri)?.use { stream ->
                    val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                    capturedBitmap = bitmap
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
            // User can try clicking again
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual Food Log", color = Color.White) },
        containerColor = Color(0xFF1C1C1E),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Photo Picker Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLow)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .clickable {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                cameraLauncher.launch(photoUri)
                            } else {
                                permissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (capturedBitmap != null) {
                        Image(
                            bitmap = capturedBitmap!!.asImageBitmap(),
                            contentDescription = "Manual Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                        Icon(Icons.Default.CameraAlt, "Retake", tint = Color.White)
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.Gray)
                            Text("Add Photo", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = foodName,
                    onValueChange = { foodName = it },
                    label = { Text("Food Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Gray
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it },
                        label = { Text("Kcal") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    OutlinedTextField(
                        value = fiber,
                        onValueChange = { fiber = it },
                        label = { Text("Fiber (g)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = starch,
                        onValueChange = { starch = it },
                        label = { Text("Starch (g)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    OutlinedTextField(
                        value = sugar,
                        onValueChange = { sugar = it },
                        label = { Text("Sugar (g)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        NutrientData(
                            foodName = foodName,
                            calories = calories.toIntOrNull() ?: 0,
                            fiber = fiber.toFloatOrNull() ?: 0f,
                            resistantStarch = starch.toFloatOrNull() ?: 0f,
                            sugar = sugar.toFloatOrNull() ?: 0f
                        ),
                        capturedBitmap
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Text("Log Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
