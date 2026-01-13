package de.kolping.cockpit.recorderapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import de.kolping.cockpit.mapping.android.ui.RecorderScreen

class RecorderAppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle back button press from the recorder screen
        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
        
        setContent {
            RecorderScreen(onBack = { finish() })
        }
    }
}
