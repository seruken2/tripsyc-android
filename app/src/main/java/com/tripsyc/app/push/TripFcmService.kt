package com.tripsyc.app.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tripsyc.app.MainActivity
import com.tripsyc.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TripFcmService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "tripsyc_notifications"
        const val CHANNEL_NAME = "Tripsyc"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Register with backend when token refreshes
        CoroutineScope(Dispatchers.IO).launch {
            try {
                com.tripsyc.app.data.api.ApiClient.apiService.registerDeviceToken(
                    mapOf("token" to token, "platform" to "android")
                )
            } catch (e: Exception) { /* best-effort */ }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: return
        val tripId = message.data["tripId"]
        val type = message.data["type"]?.lowercase()
        val inviteId = message.data["inviteId"]

        // App is in foreground — route to the in-app banner instead of
        // posting a system notification, which would yank the user's
        // attention away from whatever they're already doing. Matches
        // iOS's willPresent suppression for the foreground case.
        if (InAppBanner.isForeground()) {
            InAppBanner.post(InAppBanner.Event(title = title, body = body, tripId = tripId))
            return
        }

        showNotification(
            title = title,
            body = body,
            tripId = tripId,
            inviteId = inviteId.takeIf { type == "invite" }
        )
    }

    private fun showNotification(
        title: String,
        body: String,
        tripId: String?,
        inviteId: String? = null
    ) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)

        val notificationId = System.currentTimeMillis().toInt()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            tripId?.let { putExtra("tripId", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Inline Accept / Decline buttons on invite notifications —
        // matches the iOS notification category. Receiver fires the
        // same respondToInvite API and dismisses the notification.
        if (inviteId != null) {
            val acceptIntent = Intent(this, InviteActionReceiver::class.java).apply {
                action = InviteActionReceiver.ACTION_ACCEPT
                putExtra(InviteActionReceiver.EXTRA_INVITE_ID, inviteId)
                tripId?.let { putExtra(InviteActionReceiver.EXTRA_TRIP_ID, it) }
                putExtra(InviteActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val declineIntent = Intent(this, InviteActionReceiver::class.java).apply {
                action = InviteActionReceiver.ACTION_DECLINE
                putExtra(InviteActionReceiver.EXTRA_INVITE_ID, inviteId)
                tripId?.let { putExtra(InviteActionReceiver.EXTRA_TRIP_ID, it) }
                putExtra(InviteActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val acceptPending = PendingIntent.getBroadcast(
                this, notificationId * 2, acceptIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val declinePending = PendingIntent.getBroadcast(
                this, notificationId * 2 + 1, declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder
                .addAction(0, "Accept", acceptPending)
                .addAction(0, "Decline", declinePending)
        }

        manager.notify(notificationId, builder.build())
    }
}
