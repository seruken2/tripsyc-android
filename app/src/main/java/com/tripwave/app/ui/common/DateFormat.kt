package com.tripwave.app.ui.common

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pretty-prints a Tripwave locked-date range (the wire format the
 * server uses for DecisionLock.lockedValue and a few other places):
 *
 *   "2026-05-20 to 2026-05-30" → "May 20 – May 30, 2026"
 *   "2026-05-20"               → "May 20, 2026"
 *   unparseable                → input string unchanged
 *
 * Mirrors iOS's formatLockedDate so every surface that surfaces a
 * locked date range — Overview, Dates, TripSummary, GroupRewind —
 * reads the same.
 */
fun formatLockedDateRange(value: String?): String {
    if (value.isNullOrBlank()) return value.orEmpty()
    val parser = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val display = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    val parts = value.split(" to ")
    return when (parts.size) {
        2 -> {
            val s = runCatching { LocalDate.parse(parts[0].trim(), parser) }.getOrNull()
            val e = runCatching { LocalDate.parse(parts[1].trim(), parser) }.getOrNull()
            if (s != null && e != null) "${display.format(s)} – ${display.format(e)}, ${e.year}" else value
        }
        1 -> {
            val s = runCatching { LocalDate.parse(parts[0].trim(), parser) }.getOrNull()
            if (s != null) "${display.format(s)}, ${s.year}" else value
        }
        else -> value
    }
}

/**
 * Pretty-prints a single yyyy-MM-dd string with year: "2026-05-20" →
 * "May 20, 2026". Returns the input unchanged when parsing fails.
 */
fun formatIsoDate(value: String?): String {
    if (value.isNullOrBlank()) return value.orEmpty()
    val parser = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val display = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    val parsed = runCatching { LocalDate.parse(value.trim(), parser) }.getOrNull()
    return parsed?.let { "${display.format(it)}, ${it.year}" } ?: value
}
