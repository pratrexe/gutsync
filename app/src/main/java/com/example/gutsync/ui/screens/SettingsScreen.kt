package com.example.gutsync.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.rememberAsyncImagePainter
import com.example.gutsync.GutSyncViewModel
import com.example.gutsync.data.auth.AuthSession
import com.example.gutsync.data.storage.CsvHelper
import com.example.gutsync.ui.theme.SurfaceContainerLow
import com.example.gutsync.ui.theme.SurfaceContainerLowest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun SettingsScreen(
    session: AuthSession,
    onConnectDrive: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: GutSyncViewModel = viewModel()
) {
    val appData by viewModel.appData.collectAsState()
    val profile = appData.profile
    val context = LocalContext.current

    var fiberGoal by remember(profile.fiberGoal) { mutableStateOf(profile.fiberGoal.toString()) }
    var polyphenolGoal by remember(profile.polyphenolGoal) { mutableStateOf(profile.polyphenolGoal.toString()) }
    var starchGoal by remember(profile.resistantStarchGoal) { mutableStateOf(profile.resistantStarchGoal.toString()) }
    var showTerms by remember { mutableStateOf(false) }

    val csvImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importFromCsv(context, it) }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            try {
                val csvData = CsvHelper.exportMealsToCsv(appData.meals)
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(csvData.toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Spacer(modifier = Modifier.height(32.dp)) }

        // Profile Header
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (session.photoUrl != null) {
                        Image(
                            painter = rememberAsyncImagePainter(session.photoUrl),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = session.displayName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                session.email?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Goal Setting Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "DAILY TARGETS",
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )

                    GoalInputRow(
                        label = "Fiber Goal (g)",
                        value = fiberGoal,
                        onValueChange = { fiberGoal = it }
                    )

                    GoalInputRow(
                        label = "Polyphenol Goal (mg)",
                        value = polyphenolGoal,
                        onValueChange = { polyphenolGoal = it }
                    )

                    GoalInputRow(
                        label = "Resistant Starch (g)",
                        value = starchGoal,
                        onValueChange = { starchGoal = it }
                    )

                    Button(
                        onClick = {
                            viewModel.updateGoals(
                                fiber = fiberGoal.toIntOrNull() ?: 30,
                                polyphenols = polyphenolGoal.toIntOrNull() ?: 500,
                                starch = starchGoal.toIntOrNull() ?: 15
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Targets", fontSize = 14.sp)
                    }
                }
            }
        }

        // Account Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = if (session.isDriveConnected) Icons.Default.Cloud else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (session.isDriveConnected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (session.isDriveConnected) "Cloud Sync Active" else "Local Only",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = if (session.isDriveConnected) "Data synced with Google Drive" else "Sync with Drive to save history",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { csvExportLauncher.launch("gutsync_meals_${System.currentTimeMillis()}.csv") }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Text("Export Data as CSV", color = Color.White, fontSize = 14.sp)
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { csvImportLauncher.launch(arrayOf("text/comma-separated-values", "text/csv")) }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Text("Import Data from CSV", color = Color.White, fontSize = 14.sp)
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTerms = true }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Text("Terms & Conditions", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }

        // Action Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!session.isDriveConnected) {
                    Button(
                        onClick = onConnectDrive,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Connect Google Drive", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Sign Out", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Made by Pratyush",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Logo by Vaibhav • Research by Aastha",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }

    if (showTerms) {
        TermsAndConditionsDialog(onDismiss = { showTerms = false })
    }
}

@Composable
fun TermsAndConditionsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Terms & Conditions", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                item {
                    Text(
                        text = """
                            1. Acceptance of Terms
                            By using GutSync, you agree to these terms. GutSync provides microbiome insights based on AI analysis and user input.

                            2. Not Medical Advice
                            The information provided by Maya and the Gut Intelligence Engine (GIE) is for educational purposes only. It is not a substitute for professional medical advice, diagnosis, or treatment.

                            3. Data Privacy
                            Your nutritional data is stored locally or in your personal Google Drive. We do not sell your personal health logs.

                            4. User Responsibility
                            Accuracy of analysis depends on the quality of your input. Use the barcode scanner or clear photos for best results.

                            5. Credits & Ownership
                            - Developed by: Pratyush
                            - Visual Identity & Logo: Vaibhav
                            - Scientific Research & Logic: Aastha

                            6. Limitation of Liability
                            GutSync is provided "as is". We are not responsible for dietary decisions made based on app insights.
                        """.trimIndent(),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Understood", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF1C1C1E),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.8f)
    )
}

@Composable
fun GoalInputRow(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = 14.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(100.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
            ),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )
    }
}
