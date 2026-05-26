package com.tripwave.app.push

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.tripwave.app.R
import com.tripwave.app.data.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles the inline reply action on chat notifications. Pulls the
 * RemoteInput text, POSTs it to /api/chat with the source tripId, and
 * dismisses the notification on success. Mirrors the iOS chat-reply
 * notification category (UNNotificationAction with .destructive=false).
 */
class ChatReplyReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REPLY = "com.tripwave.app.CHAT_REPLY"
        const val EXTRA_TRIP_ID = "tripId"
        const val EXTRA_NOTIFICATION_ID = "notificationId"
        const val KEY_REPLY_TEXT = "key_reply_text"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent) ?: return
        val text = remoteInput.getCharSequence(KEY_REPLY_TEXT)?.toString()?.trim()
        if (text.isNullOrEmpty()) return
        val tripId = intent.getStringExtra(EXTRA_TRIP_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.apiService.sendMessage(
                    mapOf("tripId" to tripId, "text" to text)
                )
                if (notificationId >= 0) {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(notificationId)
                }
            } catch (_: Exception) {
                // Surface the failure so the user knows their reply
                // didn't send — they can tap the new notification to
                // retry inside the app.
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notif = NotificationCompat.Builder(context, TripFcmService.CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Couldn't send reply")
                    .setContentText("Open Tripwave to try again.")
                    .setAutoCancel(true)
                    .build()
                nm.notify(System.currentTimeMillis().toInt(), notif)
            }
            pending.finish()
        }
    }
}
