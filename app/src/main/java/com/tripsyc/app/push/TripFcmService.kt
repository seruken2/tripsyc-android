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

        // App is in foreground — route to the in-app banner instead of
        // posting a system notification, which would yank the user's
        // attention away from whatever they're already doing. Matches
        // iOS's willPresent suppression for the foreground case.
        if (InAppBanner.isForeground()) {
            InAppBanner.post(InAppBanner.Event(title = title, body = body, tripId = tripId))
            return
        }

        showNotification(title, body, tripId)
    }

    private fun showNotification(title: String, body: String, tripId: String?) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            tripId?.let { putExtra("tripId", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
