package com.tripsyc.app

import android.app.Application
import com.tripsyc.app.data.api.ApiClient

class TripsycApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ApiClient.initialize(this)
    }
}
