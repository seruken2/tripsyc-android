package com.tripwave.app.ui.trip.destinations

/**
 * Curated fallback photo URLs for destinations whose imageUrl is
 * missing or empty. Kotlin port of the iOS CityPhotoFallback +
 * matches the web GENERIC_TRAVEL fallback in src/lib/city-photos.ts
 * (extended with 5 alternates so different cards get visual variety).
 *
 * Picked by hashing the city string so the same destination always
 * gets the same crop (no flicker on re-compose) while different
 * destinations rotate through the alternates.
 *
 * We deliberately don't ship a 200-city table to native — that's a
 * lot of code duplication and Kotlin can't reuse the TypeScript
 * curated map. If we ever want richer parity, the right move is a
 * server endpoint returning city-photo URLs from the existing web
 * library; until then, generic-travel covers the "no blank hero"
 * requirement.
 */
fun cityPhotoFallback(city: String): String {
    var h = 2_166_136_261L
    for (b in city.lowercase().toByteArray()) {
        h = h xor (b.toLong() and 0xff)
        h = (h * 16_777_619L) and 0xFFFFFFFFL
    }
    val idx = (h % PHOTOS.size).toInt()
    return PHOTOS[if (idx < 0) idx + PHOTOS.size else idx]
}

/** All from images.unsplash.com — same CDN format the web uses. */
private val PHOTOS = listOf(
    "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&q=80&auto=format&fit=crop",
    "https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=800&q=80&auto=format&fit=crop",
    "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=800&q=80&auto=format&fit=crop",
    "https://images.unsplash.com/photo-1530789253388-582c481c54b0?w=800&q=80&auto=format&fit=crop",
    "https://images.unsplash.com/photo-1473625247510-8ceb1760943f?w=800&q=80&auto=format&fit=crop",
    "https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=800&q=80&auto=format&fit=crop",
)
