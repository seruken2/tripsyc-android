package com.tripsyc.app.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.PendingInvite
import com.tripsyc.app.data.api.models.Trip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TripsState(
    val trips: List<Trip> = emptyList(),
    val pendingInvites: List<PendingInvite> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
) {
    val filteredTrips: List<Trip>
        get() = if (searchQuery.isBlank()) trips
        else trips.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.approxMonth?.contains(searchQuery, ignoreCase = true) == true
        }
}

class TripsViewModel : ViewModel() {

    private val _state = MutableStateFlow(TripsState())
    val state: StateFlow<TripsState> = _state.asStateFlow()

    init {
        loadTrips()
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
