package com.tripsyc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.tripsyc.app.navigation.AppNavigation
import com.tripsyc.app.ui.theme.Chalk50
import com.tripsyc.app.ui.theme.TripsycTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TripsycTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Chalk50
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
