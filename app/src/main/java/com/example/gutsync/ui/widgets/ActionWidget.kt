package com.example.gutsync.ui.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.gutsync.MainActivity

class ActionWidget : GlanceAppWidget() {
    companion object {
        val ACTION_KEY = ActionParameters.Key<String>("action")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color(0xFF0A0A0A))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionSquare(
                        text = "Snap Photo",
                        icon = "📷",
                        action = "SCAN_FOOD",
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Spacer(modifier = GlanceModifier.width(12.dp))
                    ActionSquare(
                        text = "Scan Barcode",
                        icon = "🔳",
                        action = "SCAN_QR",
                        modifier = GlanceModifier.defaultWeight()
                    )
                }
            }
        }
    }

    @Composable
    private fun ActionSquare(
        text: String,
        icon: String,
        action: String,
        modifier: GlanceModifier = GlanceModifier
    ) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .background(Color(0xFF1C1C1E))
                .cornerRadius(24.dp)
                .clickable(
                    actionStartActivity<MainActivity>(
                        parameters = actionParametersOf(ACTION_KEY to action)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = icon,
                    style = TextStyle(fontSize = 32.sp)
                )
                Spacer(modifier = GlanceModifier.height(12.dp))
                Text(
                    text = text,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}
