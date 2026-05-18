package com.tripsyc.app.data

import com.tripsyc.app.data.api.models.Trip
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-wide event bus for trip-shape changes. Any screen that mutates
 * a Trip (settings save, lock toggle, etc.) emits the updated trip
 * here; screens displaying the same trip subscribe and refresh their
 * local copy so the new name / cover / approxMonth shows up
 * everywhere without a manual reload.
 *
 * Matches iOS's NotificationCenter `.tripsycTripUpdated` pattern.
 */
object TripEventBus {
    private val _tripUpdates = MutableSharedFlow<Trip>(extraBufferCapacity = 8)
    val tripUpdates: SharedFlow<Trip> = _tripUpdates.asSharedFlow()

    suspend fun emit(trip: Trip) {
        _tripUpdates.emit(trip)
    }

    fun tryEmit(trip: Trip) {
        _tripUpdates.tryEmit(trip)
    }
}
