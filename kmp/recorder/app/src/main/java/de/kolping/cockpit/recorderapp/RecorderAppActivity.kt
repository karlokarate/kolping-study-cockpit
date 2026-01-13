package de.kolping.cockpit.recorderapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import de.kolping.cockpit.mapping.android.ui.RecorderScreen

class RecorderAppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecorderScreen(onBack = { /* no-op */ })
        }
    }
}
