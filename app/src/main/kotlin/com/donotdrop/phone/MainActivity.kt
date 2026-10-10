package com.donotdrop.phone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

private enum class Screen { Main, Calibration, Settings, History }

class MainActivity : ComponentActivity() {
    private val app get() = application as App

    // 알림 권한은 거부돼도 서비스는 돈다(알림만 숨겨짐). 결과와 상관없이 감시를 시작한다.
    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { startIfEnabled() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (app.settings.onboarded) ensureRunning()
        setContent {
            MaterialTheme {
                var onboarded by remember { mutableStateOf(app.settings.onboarded) }
                var screen by remember { mutableStateOf(Screen.Main) }
                val toMain = { screen = Screen.Main }
                Column(Modifier.safeDrawingPadding()) {
                    Box(Modifier.weight(1f)) {
                        if (!onboarded) OnboardingScreen { app.settings.onboarded = true; onboarded = true; ensureRunning() }
                        else when (screen) {
                            Screen.Main -> MainScreen(app.monitor, { screen = Screen.Calibration }, { screen = Screen.Settings }, { screen = Screen.History })
                            Screen.Calibration -> CalibrationScreen(app.monitor) { app.settings.saveThresholds(app.monitor); toMain() }
                            Screen.Settings -> SettingsScreen(app.settings, app.monitor, toMain)
                            Screen.History -> HistoryScreen(app.history, toMain)
                        }
                    }
                    if (onboarded && (screen == Screen.Main || screen == Screen.History)) AdSlot()
                }
            }
        }
    }

    private fun ensureRunning() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS) else startIfEnabled()
    }

    private fun startIfEnabled() { if (app.settings.monitorEnabled) setMonitoring(this, true) }
}
