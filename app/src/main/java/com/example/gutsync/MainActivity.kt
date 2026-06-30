package com.example.gutsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.gutsync.data.auth.AccountType
import com.example.gutsync.data.auth.AuthSession
import com.example.gutsync.data.auth.GoogleAuthHelper
import com.example.gutsync.data.auth.SessionManager
import com.example.gutsync.ui.components.LiquidBackground
import com.example.gutsync.ui.screens.*
import com.example.gutsync.ui.theme.GutsyncTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        setContent {
            val viewModel: GutSyncViewModel = viewModel()
            var currentSession by remember { mutableStateOf(sessionManager.getSession()) }

            val googleSignInLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    if (account != null) {
                        viewModel.syncWithDrive(this@MainActivity, account) { newSession ->
                            currentSession = newSession
                        }
                    }
                } catch (e: ApiException) {
                    e.printStackTrace()
                }
            }

            GutsyncTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    LiquidBackground()
                    
                    if (currentSession.isLoggedIn) {
                        MainNavigation(
                            session = currentSession,
                            onConnectDrive = {
                                googleSignInLauncher.launch(GoogleAuthHelper.getSignInIntent(this@MainActivity))
                            },
                            onSignOut = {
                                sessionManager.clearSession()
                                currentSession = AuthSession()
                            },
                            viewModel = viewModel
                        )
                    } else {
                        LoginScreen(
                            onSignInWithGoogle = {
                                googleSignInLauncher.launch(GoogleAuthHelper.getSignInIntent(this@MainActivity))
                            },
                            onContinueOffline = {
                                val offlineSession = AuthSession(
                                    isLoggedIn = true,
                                    accountType = AccountType.OFFLINE,
                                    displayName = "Guest User"
                                )
                                sessionManager.saveSession(offlineSession)
                                currentSession = offlineSession
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainNavigation(
    session: AuthSession,
    onConnectDrive: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: GutSyncViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        NavigationItem("Home", Icons.Default.Home),
        NavigationItem("Log", Icons.Default.AddCircle),
        NavigationItem("Trends", Icons.Default.BarChart),
        NavigationItem("Guide", Icons.AutoMirrored.Filled.MenuBook),
        NavigationItem("AI", Icons.Default.AutoAwesome)
    )

    // Scroll state for hiding navigation
    var navVisible by remember { mutableStateOf(true) }
    val navOffset by animateFloatAsState(
        targetValue = if (navVisible) 0f else 300f,
        label = "nav_offset"
    )

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -15) {
                    if (navVisible) navVisible = false
                } else if (available.y > 15) {
                    if (!navVisible) navVisible = true
                }
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        Scaffold(
            containerColor = Color.Transparent
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = Color.Transparent
            ) {
                when (selectedTab) {
                    0 -> DashboardScreen(
                        session = session,
                        onConnectDrive = onConnectDrive,
                        onSignOut = onSignOut,
                        viewModel = viewModel
                    )
                    1 -> MealLoggerScreen(viewModel = viewModel)
                    2 -> TrendsScreen(viewModel = viewModel)
                    3 -> InsightsScreen()
                    4 -> AskGeminiScreen(viewModel = viewModel)
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .offset { IntOffset(0, navOffset.roundToInt()) }
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            DynamicIslandNav(
                tabs = tabs,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
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
            .height(64.dp)
            .widthIn(max = 350.dp)
            .clip(CircleShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(20.dp)
                .background(Color.Black.copy(alpha = 0.4f))
        )

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                )
            ),
            shape = CircleShape
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
