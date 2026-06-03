package com.tripwave.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.tripwave.app.MainActivity

/**
 * Home-screen widget showing the user's next upcoming trip with a
 * live countdown. Reads from Glance's PreferencesGlanceStateDefinition,
 * which the app's existing TripPrefsStore writes to whenever the trips
 * list refreshes — keeps the widget data path identical to the in-app
 * data path so we don't double-source state.
 *
 * Coral brand color matches the web app. Tap → opens MainActivity,
 * which routes to /trips by default.
 *
 * Sizing constraints live in res/xml/next_trip_widget_info.xml — kept
 * to a 2x2 minimum so the title + countdown fit comfortably.
 */
class NextTripWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetBody(context)
            }
        }
    }

    @Composable
    private fun WidgetBody(context: Context) {
        val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
        val tripName = prefs[KEY_TRIP_NAME] ?: "No upcoming trips"
        val daysUntil = prefs[KEY_TRIP_DAYS_UNTIL]?.toIntOrNull()
        val destination = prefs[KEY_TRIP_DESTINATION] // optional, may be null

        val openApp = actionStartActivity(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp)
                .background(ColorProvider(BRAND_CORAL))
                .cornerRadius(20.dp)
                .clickable(openApp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "TRIPWAVE",
                    style = TextStyle(
                        color = ColorProvider(Color(0xCCFFFFFF)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = tripName,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                if (destination != null) {
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text = destination,
                        style = TextStyle(
                            color = ColorProvider(Color(0xCCFFFFFF)),
                            fontSize = 12.sp,
                        ),
                    )
                }
                Spacer(GlanceModifier.height(8.dp))
                if (daysUntil != null && daysUntil >= 0) {
                    Text(
                        text = countdownLabel(daysUntil),
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                } else {
                    Text(
                        text = "Tap to open Tripwave",
                        style = TextStyle(
                            color = ColorProvider(Color(0xCCFFFFFF)),
                            fontSize = 11.sp,
                        ),
                    )
                }
            }
        }
    }

    companion object {
        // Coral brand color, matches tailwind coral-500 used everywhere on web.
        private val BRAND_CORAL = Color(0xFFE8654A)

        // Preferences keys the app writes to populate the widget. Kept
        // here next to the reader so they don't drift; the app-side
        // writer in TripPrefsStore imports these.
        val KEY_TRIP_NAME = stringPreferencesKey("widget_trip_name")
        val KEY_TRIP_DAYS_UNTIL = stringPreferencesKey("widget_trip_days_until")
        val KEY_TRIP_DESTINATION = stringPreferencesKey("widget_trip_destination")

        private fun countdownLabel(days: Int): String = when (days) {
            0 -> "Today!"
            1 -> "Tomorrow"
            in 2..30 -> "$days days to go"
            else -> "$days days"
        }
    }
}

/**
 * AppWidget receiver — registered in the manifest. Just hands off to
 * the Glance widget; doesn't need any custom logic.
 */
class NextTripWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextTripWidget()
}
