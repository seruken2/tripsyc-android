package com.tripsyc.app.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.PendingInvite
import com.tripsyc.app.data.api.models.Trip
import com.tripsyc.app.data.prefs.TripPrefsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TripsState(
    val trips: List<Trip> = emptyList(),
    val pendingInvites: List<PendingInvite> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val pinnedIds: Set<String> = emptySet(),
    val archivedIds: Set<String> = emptySet(),
    val showArchived: Boolean = false
) {
    /// Active visible list: search-filtered, then archived rows dropped
    /// unless `showArchived` is on, then pinned ones floated to top.
    val filteredTrips: List<Trip>
        get() {
            val searched = if (searchQuery.isBlank()) trips
            else trips.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.approxMonth?.contains(searchQuery, ignoreCase = true) == true
            }
            val visible = if (showArchived) searched
            else searched.filter { it.id !in archivedIds }
            val (pinned, rest) = visible.partition { it.id in pinnedIds }
            return pinned + rest
        }

    val archivedCount: Int get() = trips.count { it.id in archivedIds }
}

class TripsViewModel : ViewModel() {

    private val _state = MutableStateFlow(
        TripsState(
            pinnedIds = TripPrefsStore.pinnedIds(),
            archivedIds = TripPrefsStore.archivedIds()
        )
    )
    val state: StateFlow<TripsState> = _state.asStateFlow()

    init {
        loadTrips()
    }

    fun togglePinned(tripId: String) {
        val updated = TripPrefsStore.togglePinned(tripId)
        _state.value = _state.value.copy(pinnedIds = updated)
    }

    fun toggleArchived(tripId: String) {
        val updated = TripPrefsStore.toggleArchived(tripId)
        _state.value = _state.value.copy(archivedIds = updated)
    }

    fun setShowArchived(show: Boolean) {
        _state.value = _state.value.copy(showArchived = show)
    }

    fun loadTrips() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val trips = ApiClient.apiService.getTrips()
                val invites = try {
                    ApiClient.apiService.getPendingInvites()
                } catch (e: Exception) {
                    emptyList()
                }
                _state.value = _state.value.copy(
                    trips = trips,
                    pendingInvites = invites,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load trips"
                )
            }
        }
    }

    fun refresh() {
        loadTrips()
    }

    fun updateSearch(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun insertTrip(trip: Trip) {
        val current = _state.value.trips.toMutableList()
        current.removeAll { it.id == trip.id }
        current.add(0, trip)
        _state.value = _state.value.copy(trips = current)
    }

    fun removePendingInvite(id: String) {
        _state.value = _state.value.copy(
            pendingInvites = _state.value.pendingInvites.filter { it.id != id }
        )
    }

    fun acceptInvite(inviteId: String, onResult: (Trip?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.respondToInvite(
                    mapOf("inviteId" to inviteId, "action" to "accept")
                )
                removePendingInvite(inviteId)
                if (response.tripId != null) {
                    val trip = ApiClient.apiService.getTrip(response.tripId)
                    insertTrip(trip)
                    onResult(trip)
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }

    fun declineInvite(inviteId: String) {
        viewModelScope.launch {
            try {
                ApiClient.apiService.respondToInvite(
                    mapOf("inviteId" to inviteId, "action" to "decline")
                )
                removePendingInvite(inviteId)
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
