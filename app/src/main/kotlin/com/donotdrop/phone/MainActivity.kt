package com.donotdrop.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    private lateinit var monitor: FallMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        monitor = FallMonitor(this)
        setContent {
            MaterialTheme {
                var calibrating by remember { mutableStateOf(false) }
                androidx.compose.foundation.layout.Box(Modifier.safeDrawingPadding()) {
                    if (calibrating) CalibrationScreen(monitor) { calibrating = false }
                    else MainScreen(monitor) { calibrating = true }
                }
            }
        }
    }

    override fun onPause() { super.onPause(); monitor.stop() } // MVP: 앱이 켜진 상태에서만 동작

    override fun onDestroy() { super.onDestroy(); monitor.release() }
}
