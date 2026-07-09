package com.example.gutsync.ui.widgets

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.gutsync.data.storage.GutSyncRepository

class StreakWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = GutSyncRepository(context)
        
        provideContent {
            val appData by repository.appData.collectAsState()
            val streak = appData.profile.streakCount

            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color(0xFF0A0A0A))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(Color(0xFF1C1C1E))
                            .cornerRadius(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🔥",
                                style = TextStyle(fontSize = 32.sp)
                            )
                            Text(
                                text = streak.toString(),
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "STREAK",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFA1A1AA)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
