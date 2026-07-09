package com.example.gutsync.ui.widgets

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.gutsync.MainActivity
import com.example.gutsync.data.storage.GutSyncRepository

class ActionWidget : GlanceAppWidget() {
    companion object {
        val ACTION_KEY = ActionParameters.Key<String>("action")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = GutSyncRepository(context)

        provideContent {
            val appData by repository.appData.collectAsState()
            val latestMeals = appData.meals.takeLast(2).reversed()

            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color(0xFF0A0A0A))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "QUICK LOG",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier.padding(bottom = 8.dp)
                    )

                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        Button(
                            text = "Scan QR",
                            onClick = actionStartActivity<MainActivity>(
                                parameters = actionParametersOf(ACTION_KEY to "SCAN_QR")
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Button(
                            text = "Scan Food",
                            onClick = actionStartActivity<MainActivity>(
                                parameters = actionParametersOf(ACTION_KEY to "SCAN_FOOD")
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(16.dp))

                    Text(
                        text = "RECENT ACTIVITY",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFA1A1AA)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = GlanceModifier.padding(bottom = 8.dp)
                    )

                    // Details Boxes
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        repeat(2) { index ->
                            val meal = latestMeals.getOrNull(index)
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .background(Color(0xFF1C1C1E))
                                    .padding(8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (meal != null) {
                                    Text(
                                        text = meal.nutrients.foodName,
                                        style = TextStyle(color = ColorProvider(Color.White), fontSize = 14.sp)
                                    )
                                } else {
                                    Text(
                                        text = "No recent activity",
                                        style = TextStyle(color = ColorProvider(Color(0xFF48484A)), fontSize = 12.sp)
                                    )
                                }
                            }
                            if (index == 0) Spacer(modifier = GlanceModifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}
