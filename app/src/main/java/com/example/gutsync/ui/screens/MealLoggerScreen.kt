package com.example.gutsync.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.material.icons.filled.CameraAlt
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.gutsync.GutSyncViewModel
import com.example.gutsync.UiState
import com.example.gutsync.data.MicrobeImpactCalculator
import com.example.gutsync.data.NutrientData
import com.example.gutsync.ui.theme.SurfaceContainerLow
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MealLoggerScreen(viewModel: GutSyncViewModel = viewModel()) {
    var searchQuery by remember { mutableStateOf("") }
    val analyzedFood by viewModel.analyzedFood.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val capturedImage by viewModel.capturedImage.collectAsState()

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.setCapturedImage(bitmap)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Search Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "LOG NUTRITION (AI ANALYZED)",
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Describe or capture your meal...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = {
                        if (uiState is UiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            if (analyzedFood != null && searchQuery.isBlank()) {
                                Button(
                                    onClick = { viewModel.addAnalyzedFood() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Confirm")
                                }
                            } else {
                                TextButton(onClick = { viewModel.analyzeFood(searchQuery, capturedImage); searchQuery = "" }) {
                                    Text("Analyze", color = Color.White)
                                }
                            }
                        }
                    }
                )
            }
        }

        // Score & Details Bento
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ImpactScoreCard(analyzedFood, modifier = Modifier.weight(1f))
                BioticDensityCard(analyzedFood, modifier = Modifier.weight(1f))
            }
        }

        // Image Spotlight (Now with Camera Action)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(24.dp))
                    .clickable { cameraLauncher.launch() },
                contentAlignment = Alignment.Center
            ) {
                if (capturedImage != null) {
                    Image(
                        bitmap = capturedImage!!.asImageBitmap(),
                        contentDescription = "Captured Meal",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Overlay to retake
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
                            text = "Tap to Capture Meal",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Recent Items
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(text = "Recent Items", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(text = "View All", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        items(recentItems) { item ->
            RecentItemRow(item)
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun ImpactScoreCard(nutrients: NutrientData?, modifier: Modifier = Modifier) {
    val impact = nutrients?.let { MicrobeImpactCalculator.calculateImpact(it) }
    val score = impact?.first ?: 0
    val shiftText = if (nutrients != null) {
        "This meal promotes beneficial microbes and improves gut integrity."
    } else {
        "Capture or describe a meal to see its microbial impact."
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C2E)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.height(180.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "MICROBE IMPACT",
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
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun BioticDensityCard(nutrients: NutrientData?, modifier: Modifier = Modifier) {
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
            BioticRow("Fiber", "${nutrients?.fiber ?: 0}g")
            BioticRow("Polyphenols", "${nutrients?.polyphenols ?: 0}mg")
            BioticRow("Fermented", "${nutrients?.fermentedCultures ?: 0} Active")
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
fun RecentItemRow(item: RecentItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C2E)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().alpha(if (item.faded) 0.4f else 1f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = item.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(text = "${item.time} • ${item.calories} kcal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(
                onClick = {},
                modifier = Modifier.background(Color.White, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
            }
        }
    }
}

data class RecentItem(val name: String, val time: String, val calories: Int, val faded: Boolean = false)

val recentItems = listOf(
    RecentItem("Steel Cut Oats", "Logged 4h ago", 250),
    RecentItem("Greek Yogurt", "Logged 8h ago", 120),
    RecentItem("Kombucha (Raw)", "Logged Yesterday", 30, faded = true)
)
