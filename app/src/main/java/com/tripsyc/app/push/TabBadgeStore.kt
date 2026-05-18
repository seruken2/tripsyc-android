package com.tripsyc.app.push

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global per-trip tab event badges. FCM messages with a `tab` data
 * field mark the relevant tab as having unseen activity; TripDetail's
 * onTabSelected hook clears it once the user looks at that tab.
 *
 * Compose-observable via StateFlow so TripTabBar can paint a small
 * dot indicator next to the tab icon. Matches iOS AppState.
 */
object TabBadgeStore {
    private val _badges = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val badges: StateFlow<Map<String, Set<String>>> = _badges.asStateFlow()

    fun mark(tripId: String, tab: String) {
        val current = _badges.value.toMutableMap()
        val existing = (current[tripId] ?: emptySet()).toMutableSet()
        existing.add(tab)
        current[tripId] = existing
        _badges.value = current
    }

    fun clear(tripId: String, tab: String) {
        val current = _badges.value.toMutableMap()
        val existing = current[tripId]?.toMutableSet() ?: return
        existing.remove(tab)
        if (existing.isEmpty()) current.remove(tripId) else current[tripId] = existing
        _badges.value = current
    }

    fun badgesFor(tripId: String): Set<String> = _badges.value[tripId] ?: emptySet()
}
