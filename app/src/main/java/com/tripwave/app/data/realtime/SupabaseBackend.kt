package com.tripwave.app.data.realtime

import com.tripwave.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.Realtime

/// Shared Supabase client. Lazy-initialized from BuildConfig credentials.
/// Returns null when SUPABASE_URL or SUPABASE_ANON_KEY are unset — in that
/// case callers (currently TypingChannel) fall back to the REST poll path so
/// the chat stays functional without the realtime feature.
object SupabaseBackend {
    val client: SupabaseClient? by lazy {
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_ANON_KEY
        if (url.isBlank() || key.isBlank()) return@lazy null
        runCatching {
            createSupabaseClient(supabaseUrl = url, supabaseKey = key) {
                install(Realtime)
            }
        }.getOrNull()
    }
}
