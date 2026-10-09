package com.donotdrop.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

private enum class Screen { Main, Calibration, Settings, History }

class MainActivity : ComponentActivity() {
    private lateinit var monitor: FallMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = Settings(this)
        val history = HistoryStore(this)
        monitor = FallMonitor(this).also {
            settings.applyTo(it)
            it.onLanded = history::add
        }
        setContent {
            MaterialTheme {
                var onboarded by remember { mutableStateOf(settings.onboarded) }
                var screen by remember { mutableStateOf(Screen.Main) }
                val toMain = { screen = Screen.Main }
                Box(Modifier.safeDrawingPadding()) {
                    if (!onboarded) OnboardingScreen { settings.onboarded = true; onboarded = true }
                    else when (screen) {
                        Screen.Main -> MainScreen(monitor, { screen = Screen.Calibration }, { screen = Screen.Settings }, { screen = Screen.History })
                        Screen.Calibration -> CalibrationScreen(monitor) { settings.saveThresholds(monitor); toMain() }
                        Screen.Settings -> SettingsScreen(settings, monitor, toMain)
                        Screen.History -> HistoryScreen(history, toMain)
                    }
                }
            }
        }
    }

    override fun onPause() { super.onPause(); monitor.stop() } // MVP: 앱이 켜진 상태에서만 동작

    override fun onDestroy() { super.onDestroy(); monitor.release() }
}
