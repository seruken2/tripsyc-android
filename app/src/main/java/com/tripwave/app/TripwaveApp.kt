package com.tripwave.app

import android.app.Application
import android.content.Context
import com.tripwave.app.data.api.ApiClient

class TripwaveApp : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        ApiClient.initialize(this)
    }

    companion object {
        /**
         * Static application-context handle, set in onCreate. Lets
         * singletons + ViewModels (which don't want AndroidViewModel
         * boilerplate) reach the app context without piping it
         * through every constructor. Used by
         * [com.tripwave.app.widget.NextTripWidgetUpdater] via
         * TripsViewModel.
         *
         * Nullable until Application.onCreate runs; null-check at
         * use sites is a no-op for any real runtime path (only a
         * unit test that imports TripwaveApp without starting the
         * framework would hit it).
         */
        @Volatile
        var appContext: Context? = null
            private set
    }
}
