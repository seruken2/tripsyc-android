package com.tripwave.app.data.realtime

import com.tripwave.app.data.api.ApiClient
import com.tripwave.app.data.api.models.RealtimeTokenResponse
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/// Broadcast payload shape matching iOS + web — keep the field names
/// identical so any platform can read another's events without remapping.
@Serializable
private data class TypingPayload(val userId: String, val name: String)

private data class TypingEntry(val name: String, val lastSeen: Long)

/// Realtime typing indicator over Supabase's `trip:{tripId}:typing` private
/// channel. Mirrors the iOS implementation in [ChatViewModel.startTypingPoll]:
/// throttled broadcasts on keystroke, 4-second TTL on received entries
/// (Realtime has no "stopped typing" event), token refresh ahead of the JWT's
/// 1h expiry.
///
/// Caller pattern in ChatScreen:
/// ```
/// val channel = remember { TypingChannel(tripId, scope) }
/// LaunchedEffect(tripId) { channel.start(currentUserId, currentUserName) }
/// DisposableEffect(tripId) { onDispose { channel.stop() } }
/// // …
/// channel.broadcast()  // on text change
/// channel.typingNames.collectAsState().value  // render
/// ```
class TypingChannel(
    private val tripId: String,
    private val scope: CoroutineScope,
) {
    private val _typingNames = MutableStateFlow<List<String>>(emptyList())
    val typingNames: StateFlow<List<String>> = _typingNames.asStateFlow()

    private var selfUserId: String? = null
    private var selfName: String = "Someone"

    private var channel: RealtimeChannel? = null
    private var subscribeJob: Job? = null
    private var sweepJob: Job? = null
    private var tokenRefreshJob: Job? = null
    private val entries = mutableMapOf<String, TypingEntry>()
    private var lastBroadcastMs = 0L

    /// True only when the Supabase backend is configured. If false, callers
    /// should fall back to the REST typing endpoints.
    val isAvailable: Boolean get() = SupabaseBackend.client != null

    suspend fun start(userId: String, userName: String?) {
        if (!isAvailable) return
        selfUserId = userId
        selfName = userName?.takeIf { it.isNotBlank() } ?: "Someone"

        val client = SupabaseBackend.client ?: return
        val tokenResp = fetchToken() ?: return
        scheduleTokenRefresh(tokenResp.expiresAt)

        val ch = client.channel("trip:$tripId:typing") {
            isPrivate = true
        }
        channel = ch
        // RLS gates private channel subscription — set the server-minted JWT
        // (whose `trips` claim lists this user's memberships) before subscribe().
        runCatching { ch.updateAuth(tokenResp.token) }

        subscribeJob = scope.launch {
            ch.broadcastFlow<TypingPayload>("typing").collect { p ->
                if (p.userId == selfUserId) return@collect
                synchronized(entries) {
                    entries[p.userId] = TypingEntry(p.name, System.currentTimeMillis())
                }
                publish()
            }
        }

        runCatching { ch.subscribe() }

        // Realtime broadcasts are ephemeral — no "stopped" event — so sweep
        // expired entries locally on a 1s tick.
        sweepJob = scope.launch {
            while (isActive) {
                delay(1000)
                val cutoff = System.currentTimeMillis() - TTL_MS
                val changed = synchronized(entries) {
                    val before = entries.size
                    entries.entries.removeIf { it.value.lastSeen < cutoff }
                    entries.size != before
                }
                if (changed) publish()
            }
        }
    }

    /// Broadcast a "typing" event with this user's name. Throttled so a burst
    /// of keystrokes doesn't flood the socket; one event extends the peer's
    /// TTL window by 4s.
    fun broadcast() {
        val userId = selfUserId ?: return
        val ch = channel ?: return
        val now = System.currentTimeMillis()
        if (now - lastBroadcastMs < SEND_THROTTLE_MS) return
        lastBroadcastMs = now
        scope.launch {
            runCatching {
                ch.broadcast(event = "typing", message = TypingPayload(userId, selfName))
            }
        }
    }

    fun stop() {
        subscribeJob?.cancel(); subscribeJob = null
        sweepJob?.cancel(); sweepJob = null
        tokenRefreshJob?.cancel(); tokenRefreshJob = null
        synchronized(entries) { entries.clear() }
        _typingNames.value = emptyList()
        val ch = channel
        channel = null
        if (ch != null) {
            scope.launch { runCatching { ch.unsubscribe() } }
        }
    }

    private fun publish() {
        val snapshot = synchronized(entries) {
            entries.values.map { it.name }.sorted()
        }
        _typingNames.value = snapshot
    }

    private suspend fun fetchToken(): RealtimeTokenResponse? =
        runCatching { ApiClient.apiService.getRealtimeToken() }.getOrNull()

    private fun scheduleTokenRefresh(expiresAtSec: Double) {
        tokenRefreshJob?.cancel()
        val nowSec = System.currentTimeMillis() / 1000.0
        val sleepSec = (expiresAtSec - nowSec - SAFETY_SEC).coerceAtLeast(60.0)
        tokenRefreshJob = scope.launch {
            delay((sleepSec * 1000).toLong())
            val next = fetchToken() ?: return@launch
            runCatching { channel?.updateAuth(next.token) }
            scheduleTokenRefresh(next.expiresAt)
        }
    }

    companion object {
        private const val TTL_MS = 4_000L
        private const val SEND_THROTTLE_MS = 2_000L
        private const val SAFETY_SEC = 10.0 * 60
    }
}
