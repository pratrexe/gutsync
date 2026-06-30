package com.example.gutsync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gutsync.ui.theme.SurfaceContainerHigh
import com.example.gutsync.ui.theme.SurfaceVariant

@Composable
fun InsightsScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Header
        item {
            Column {
                Text(text = "Gut-Bites", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "Bite-sized intelligence for your second brain.", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Featured Insight Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C2E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    // Background Icon Decoration
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp).align(Alignment.TopEnd).alpha(0.1f),
                        tint = Color.White
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "FEATURED INSIGHT", fontSize = 12.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Gut-Brain Axis", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(
                            text = "Your gut produces about 95% of your body's serotonin. This biochemical signaling pathway means the health of your microbiome directly dictates your emotional resilience and cognitive clarity.",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 28.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            shape = CircleShape
                        ) {
                            Text(text = "Dive Deeper", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Library Section
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(text = "Library", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(text = "12 Modules Available", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        items(libraryModules) { module ->
            LibraryModuleCard(module)
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun LibraryModuleCard(module: LibraryModule) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C2E)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier.size(48.dp).background(SurfaceVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Placeholder for module icons - using generic icons for now
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Column {
                    Text(text = module.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "${module.readTime} • ${module.category}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

data class LibraryModule(val title: String, val readTime: String, val category: String)

val libraryModules = listOf(
    LibraryModule("Microbes & Mood", "30s Read", "Psychology"),
    LibraryModule("The Fiber Gap", "45s Read", "Nutrition"),
    LibraryModule("Circadian Rhythm", "30s Read", "Biology"),
    LibraryModule("Prebiotic Syncing", "60s Read", "Science"),
    LibraryModule("Hydration Density", "20s Read", "Wellness")
)
