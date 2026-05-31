package com.tripwave.app

import android.app.Application
import com.tripwave.app.data.api.ApiClient

class TripwaveApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ApiClient.initialize(this)
    }
}
