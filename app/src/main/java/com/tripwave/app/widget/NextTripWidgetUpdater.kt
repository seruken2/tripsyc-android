package com.tripwave.app.widget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import java.util.concurrent.TimeUnit

/**
 * Pushes the next-upcoming-trip data into the widget's Glance state
 * so the home-screen widget reflects what the user has just opened
 * inside the app. Call from whichever ViewModel / Repository has the
 * authoritative trips list after a refresh.
 *
 * Concretely: when the trips API returns, find the trip with the
 * soonest future lockedDateValue and call:
 *
 *     NextTripWidgetUpdater.update(
 *       context,
 *       tripName = trip.name,
 *       startDate = trip.lockedDate,  // epoch ms, nullable
 *       destination = trip.destinationCity, // nullable
 *     )
 *
 * The widget reads back from the same preference keys and refreshes
 * automatically — no manual notifyAppWidget call needed.
 *
 * When there's no upcoming trip, pass nulls and the widget falls back
 * to the "No upcoming trips" placeholder copy.
 */
object NextTripWidgetUpdater {

    suspend fun update(
        context: Context,
        tripName: String?,
        startDateEpochMs: Long?,
        destination: String? = null,
    ) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(NextTripWidget::class.java)
        if (glanceIds.isEmpty()) return // user hasn't added the widget; nothing to do

        val daysUntil = startDateEpochMs?.let { msUntilToDays(it - System.currentTimeMillis()) }

        glanceIds.forEach { id ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs.toMutablePreferences().apply {
                    if (tripName != null) {
                        this[NextTripWidget.KEY_TRIP_NAME] = tripName
                    } else {
                        remove(NextTripWidget.KEY_TRIP_NAME)
                    }
                    if (daysUntil != null) {
                        this[NextTripWidget.KEY_TRIP_DAYS_UNTIL] = daysUntil.toString()
                    } else {
                        remove(NextTripWidget.KEY_TRIP_DAYS_UNTIL)
                    }
                    if (destination != null) {
                        this[NextTripWidget.KEY_TRIP_DESTINATION] = destination
                    } else {
                        remove(NextTripWidget.KEY_TRIP_DESTINATION)
                    }
                }
            }
        }
        NextTripWidget().updateAll(context)
    }

    private fun msUntilToDays(ms: Long): Int {
        if (ms <= 0) return 0
        return TimeUnit.MILLISECONDS.toDays(ms).toInt()
    }
}
