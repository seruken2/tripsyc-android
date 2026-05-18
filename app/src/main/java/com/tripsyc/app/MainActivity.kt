package com.tripsyc.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.prefs.TripPrefsStore
import com.tripsyc.app.navigation.AppNavigation
import com.tripsyc.app.push.InAppBanner
import com.tripsyc.app.push.InAppBannerHost
import com.tripsyc.app.ui.theme.Chalk50
import com.tripsyc.app.ui.theme.TripsycTheme

class MainActivity : ComponentActivity() {

    // Deep link / notification extras passed to navigation
    private var pendingTripId: String? = null
    private var pendingDeepLink: String? = null
    // Mutable state so the in-app-banner tap can push a trip id into
    // the same channel a notification tap uses, without rebuilding the
    // whole activity.
    private val bannerTripId = mutableStateOf<String?>(null)

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
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppNavigation(
                            pendingTripId = bannerTripId.value ?: pendingTripId,
                            pendingDeepLink = pendingDeepLink
                        )
                        Box(modifier = Modifier.align(Alignment.TopCenter)) {
                            InAppBannerHost(
                                onTapTrip = { id -> bannerTripId.value = id }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        InAppBanner.setForeground(true)
    }

    override fun onPause() {
        super.onPause()
        InAppBanner.setForeground(false)
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

        // Handle deep links: tripsyc://verify, tripsyc://join,
        // tripsyc://google-auth, https://www.tripsyc.com
        val data: Uri? = intent?.data
        if (data != null) {
            // Google Sign-In callback — drop the session cookie into
            // the cookie jar and route through navigation. The session
            // is signed by the backend, so we don't have to validate.
            if (data.scheme == "tripsyc" && data.host == "google-auth") {
                val session = data.getQueryParameter("session")
                if (!session.isNullOrBlank()) {
                    ApiClient.setSessionCookie(session)
                    // Drop the deep-link payload off the Intent before
                    // recreate(), otherwise the rebuilt MainActivity
                    // would re-enter this branch and recreate again —
                    // infinite loop.
                    intent.data = null
                    intent.replaceExtras(null as android.os.Bundle?)
                    // AppNavigation reads the session once at start.
                    // Recreate the activity so the cookie picks up and
                    // the user lands on Main instead of staying on
                    // Login with a stale "no session" startDestination.
                    recreate()
                    return
                }
            }
            pendingDeepLink = data.toString()
        }
    }
}
