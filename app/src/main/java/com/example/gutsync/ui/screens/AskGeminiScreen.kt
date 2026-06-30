package com.example.gutsync.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.rememberAsyncImagePainter
import com.example.gutsync.GutSyncViewModel
import com.example.gutsync.UiState
import com.example.gutsync.data.ChatMessage
import com.example.gutsync.data.MessageRole
import com.example.gutsync.ui.theme.SurfaceContainerLow

@Composable
fun AskGeminiScreen(viewModel: GutSyncViewModel = viewModel()) {
    var question by remember { mutableStateOf("") }
    val chatHistory by viewModel.chatHistory.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = "Today",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }

        // Chat List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(chatHistory) { message ->
                ChatBubble(message)
            }
            
            if (uiState is UiState.Loading) {
                item {
                    LoadingBubble()
                }
            }
        }

        // Input Area
        ChatInputArea(
            value = question,
            onValueChange = { question = it },
            onSend = {
                if (question.isNotBlank()) {
                    viewModel.askFoodQuestion(question)
                    question = ""
                }
            }
        )
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Role Header (Avatar + Name)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            if (!isUser) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.DarkGray, CircleShape)
                        .padding(4.dp)
                )
                Text(text = "Cooper", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            } else {
                Text(text = "Me", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Image(
                    painter = rememberAsyncImagePainter("https://lh3.googleusercontent.com/a/default-user"),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Bubble
        Surface(
            color = if (isUser) Color(0xFFD1C4E9) else Color.White, // Light purple for user, white for Cooper
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = Color.Black,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
        
        // Timestamp
        Text(
            text = message.timestamp,
            color = Color.Gray,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp, start = if (isUser) 0.dp else 4.dp, end = if (isUser) 4.dp else 0.dp)
        )
    }
}

@Composable
fun LoadingBubble() {
    Column(horizontalAlignment = Alignment.Start) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp).background(Color.DarkGray, CircleShape).padding(4.dp)
            )
            Text(text = "Cooper", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.padding(12.dp).size(20.dp),
                color = Color.Black,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
fun ChatInputArea(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 80.dp), // Extra space for nav bar
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Text Input
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier.weight(1f)
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Ask anything here..", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.Black,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                maxLines = 4
            )
        }

        // Action Menu (Floating style)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Send Button
            FloatingActionButton(
                onClick = onSend,
                containerColor = Color(0xFFD1C4E9),
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(if (value.isBlank()) Icons.Default.Add else Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}
