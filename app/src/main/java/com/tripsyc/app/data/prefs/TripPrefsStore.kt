package com.tripsyc.app.data.prefs

import android.content.Context
import android.content.SharedPreferences

/**
 * Local-only pin / archive state for the trips list. The server doesn't
 * track either today — it's UX-only state for ordering and visibility —
 * so we keep it in SharedPreferences like iOS keeps it in UserDefaults.
 *
 * Initialised once from the Application context (see TripsycApplication).
 * Lookups return empty sets if the store hasn't been initialised yet so
 * callers don't have to null-check every access.
 */
object TripPrefsStore {
    private const val FILE = "tripsyc.trip_prefs"
    private const val KEY_PINNED = "pinnedTrips"
    private const val KEY_ARCHIVED = "archivedTrips"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    fun pinnedIds(): Set<String> = prefs?.getStringSet(KEY_PINNED, emptySet()).orEmpty()

    fun archivedIds(): Set<String> = prefs?.getStringSet(KEY_ARCHIVED, emptySet()).orEmpty()

    fun togglePinned(tripId: String): Set<String> {
        val updated = pinnedIds().toMutableSet().apply {
            if (!add(tripId)) remove(tripId)
        }
        prefs?.edit()?.putStringSet(KEY_PINNED, updated)?.apply()
        return updated
    }

    fun toggleArchived(tripId: String): Set<String> {
        val updated = archivedIds().toMutableSet().apply {
            if (!add(tripId)) remove(tripId)
        }
        prefs?.edit()?.putStringSet(KEY_ARCHIVED, updated)?.apply()
        return updated
    }
}
