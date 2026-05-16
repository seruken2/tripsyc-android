package com.tripsyc.app.push

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.tripsyc.app.R
import com.tripsyc.app.data.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles Accept / Decline taps from an invite notification's action
 * buttons. Fires the same /api/pending-invites endpoint the in-app
 * accept flow uses; on failure swaps the notification body to flag
 * the failure so the user knows to retry from inside the app
 * (mirrors the iOS surface-failure-as-local-notification fix).
 */
class InviteActionReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_INVITE_ID = "inviteId"
        const val EXTRA_TRIP_ID = "tripId"
        const val EXTRA_NOTIFICATION_ID = "notificationId"
        const val ACTION_ACCEPT = "com.tripsyc.app.INVITE_ACCEPT"
        const val ACTION_DECLINE = "com.tripsyc.app.INVITE_DECLINE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val inviteId = intent.getStringExtra(EXTRA_INVITE_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val accept = intent.action == ACTION_ACCEPT
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.apiService.respondToInvite(
                    mapOf("inviteId" to inviteId, "action" to if (accept) "accept" else "decline")
                )
                if (notificationId >= 0) {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(notificationId)
                }
            } catch (_: Exception) {
                // Surface the failure so the user knows their tap
                // didn't take. Without this branch the action button
                // would just silently consume the notification.
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notif = NotificationCompat.Builder(context, TripFcmService.CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(
                        if (accept) "Couldn't accept invite"
                        else "Couldn't decline invite"
                    )
                    .setContentText("Open Tripsyc to try again.")
                    .setAutoCancel(true)
                    .build()
                nm.notify(System.currentTimeMillis().toInt(), notif)
            }
            pending.finish()
        }
    }
}
