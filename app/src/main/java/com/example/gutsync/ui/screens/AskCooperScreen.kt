package com.example.gutsync.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.rememberAsyncImagePainter
import com.example.gutsync.GutSyncViewModel
import com.example.gutsync.UiState
import com.example.gutsync.data.ChatMessage
import com.example.gutsync.data.ChatSession
import com.example.gutsync.data.MessageRole
import com.example.gutsync.data.auth.AuthSession
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AskCooperScreen(session: AuthSession, viewModel: GutSyncViewModel = viewModel()) {
    var question by remember { mutableStateOf("") }
    val currentSession by viewModel.currentSession.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val uiState by viewModel.chatState.collectAsState()
    val attachedImage by viewModel.chatImage.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    
    var showHistory by remember { mutableStateOf(false) }

    val photoUri = remember {
        val imagesDir = File(context.cacheDir, "images")
        if (!imagesDir.exists()) imagesDir.mkdirs()
        val tempFile = File.createTempFile("chat_attachment_", ".jpg", imagesDir)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            try {
                context.contentResolver.openInputStream(photoUri)?.use { stream ->
                    val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                    viewModel.setChatImage(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(currentSession.messages.size) {
        if (currentSession.messages.isNotEmpty()) {
            listState.animateScrollToItem(currentSession.messages.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Chat List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 80.dp, end = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (currentSession.messages.isEmpty() && uiState is UiState.Initial) {
                    item {
                        TryAskingSection(onSuggestionClick = { question = it })
                    }
                }

                items(currentSession.messages) { message ->
                    ChatBubble(message)
                }
                
                if (uiState is UiState.Loading) {
                    item { LoadingBubble() }
                }

                if (uiState is UiState.Error) {
                    item { ErrorBubble((uiState as UiState.Error).errorMessage) }
                }
            }

            // Redesigned Input Area
            RedesignedChatInput(
                value = question,
                onValueChange = { question = it },
                attachedImage = attachedImage,
                onAddClick = {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        cameraLauncher.launch(photoUri)
                    } else {
                        // Request permission or show dialog
                    }
                },
                onRemoveImage = { viewModel.setChatImage(null) },
                onSend = {
                    if (question.isNotBlank() || attachedImage != null) {
                        viewModel.askFoodQuestion(question)
                        question = ""
                    }
                }
            )
            Spacer(modifier = Modifier.height(100.dp)) // Space for bottom pill
        }

        // Unified Header (Top Left)
        Surface(
            color = Color.Black.copy(alpha = 0.8f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showHistory = !showHistory }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "History", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { viewModel.startNewChat() }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                VerticalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .clickable { viewModel.toggleSessionModel() }
                        .padding(end = 8.dp, start = 4.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = currentSession.summary,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Maya AI",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // History Drawer/Overlay
        if (showHistory) {
            Surface(
                color = Color.Black.copy(alpha = 0.95f),
                modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Past Conversations", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        IconButton(onClick = { showHistory = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(chatHistory.sortedByDescending { it.lastUpdated }) { session ->
                            ChatSessionHighlightCard(session) {
                                viewModel.openSession(session)
                                showHistory = false
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TryAskingSection(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Try Asking",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SuggestionChip(
                icon = Icons.Default.Timeline,
                text = "My gut health trend?",
                onClick = { onSuggestionClick("Show me my gut health trends for the last week.") }
            )
            SuggestionChip(
                icon = Icons.Default.Restaurant,
                text = "What to eat today?",
                onClick = { onSuggestionClick("Based on my profile, what should I focus on eating today?") }
            )
            SuggestionChip(
                icon = Icons.Default.Science,
                text = "Improve microbiome?",
                onClick = { onSuggestionClick("How can I improve my microbiome habits?") }
            )
            SuggestionChip(
                icon = Icons.Default.Psychology,
                text = "Stress & Gut?",
                onClick = { onSuggestionClick("How does stress impact my specific gut profile?") }
            )
        }
    }
}

@Composable
fun SuggestionChip(icon: ImageVector, text: String, onClick: () -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
            Text(text = text, color = Color.White, fontSize = 13.sp)
        }
    }
}

@Composable
fun RedesignedChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    attachedImage: android.graphics.Bitmap? = null,
    onAddClick: () -> Unit,
    onRemoveImage: () -> Unit,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (attachedImage != null) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Image(
                    bitmap = attachedImage.asImageBitmap(),
                    contentDescription = "Attachment",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = onRemoveImage,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }

        Surface(
            color = Color(0xFF1C1C1E),
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                }
                
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { Text("Ask me anything...", color = Color.Gray, fontSize = 15.sp) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 4
                )

                IconButton(
                    onClick = onSend,
                    enabled = value.isNotBlank() || attachedImage != null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (value.isNotBlank() || attachedImage != null) Color.White 
                            else Color.White.copy(alpha = 0.1f)
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send, 
                        contentDescription = "Send", 
                        tint = if (value.isNotBlank() || attachedImage != null) Color.Black else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatSessionHighlightCard(session: ChatSession, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = session.summary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(session.lastUpdated)),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val lastMsg = session.messages.lastOrNull()?.text ?: "No messages"
            Text(
                text = lastMsg,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isUser) Color.White.copy(alpha = 0.1f) else Color(0xFF2C2C2E),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            border = if (isUser) BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) else null,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(16.dp),
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun ErrorBubble(error: String) {
    Surface(
        color = Color(0x33B91C1C),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFB91C1C).copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFEF4444))
            Text(text = error, color = Color(0xFFEF4444), fontSize = 14.sp)
        }
    }
}

@Composable
fun LoadingBubble() {
    Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
        Text("Maya is thinking...", color = Color.Gray, fontSize = 14.sp)
    }
}
