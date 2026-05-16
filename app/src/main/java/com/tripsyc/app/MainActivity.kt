package com.tripsyc.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.tripsyc.app.data.prefs.TripPrefsStore
import com.tripsyc.app.navigation.AppNavigation
import com.tripsyc.app.ui.theme.Chalk50
import com.tripsyc.app.ui.theme.TripsycTheme

class MainActivity : ComponentActivity() {

    // Deep link / notification extras passed to navigation
    private var pendingTripId: String? = null
    private var pendingDeepLink: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Wire SharedPreferences-backed pin/archive store before any
        // composable can read it.
        TripPrefsStore.init(applicationContext)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            TripsycTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Chalk50
                ) {
                    AppNavigation(
                        pendingTripId = pendingTripId,
                        pendingDeepLink = pendingDeepLink
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        // Handle notification tap with tripId extra
        intent?.getStringExtra("tripId")?.let { tripId ->
            pendingTripId = tripId
        }

        // Handle deep links: tripsyc://verify, tripsyc://join, https://www.tripsyc.com
        val data: Uri? = intent?.data
        if (data != null) {
            pendingDeepLink = data.toString()
        }
    }
}
