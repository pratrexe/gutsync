package com.example.gutsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.gutsync.ui.components.LiquidBackground
import com.example.gutsync.ui.screens.AskGeminiScreen
import com.example.gutsync.ui.screens.DashboardScreen
import com.example.gutsync.ui.screens.InsightsScreen
import com.example.gutsync.ui.screens.MealLoggerScreen
import com.example.gutsync.ui.screens.TrendsScreen
import com.example.gutsync.ui.theme.GutsyncTheme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GutsyncTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    LiquidBackground()
                    MainNavigation()
                }
            }
        }
    }
}

@Composable
fun MainNavigation() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        NavigationItem("Home", Icons.Default.Home),
        NavigationItem("Log", Icons.Default.AddCircle),
        NavigationItem("Trends", Icons.Default.BarChart),
        NavigationItem("Guide", Icons.AutoMirrored.Filled.MenuBook),
        NavigationItem("AI", Icons.Default.AutoAwesome)
    )

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            DynamicIslandNav(
                tabs = tabs,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = Color.Transparent
        ) {
            when (selectedTab) {
                0 -> DashboardScreen()
                1 -> MealLoggerScreen()
                2 -> TrendsScreen()
                3 -> InsightsScreen()
                4 -> AskGeminiScreen()
            }
        }
    }
}

@Composable
fun DynamicIslandNav(
    tabs: List<NavigationItem>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        // Frosted Glass Effect
        Surface(
            modifier = Modifier
                .height(64.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f)),
            color = Color.Transparent,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEachIndexed { index, item ->
                    val isSelected = selectedTab == index
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else Color.Gray,
                        label = "icon_color"
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(
                                color = if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent
                            )
                            .clickable { onTabSelected(index) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}


data class NavigationItem(val label: String, val icon: ImageVector)
