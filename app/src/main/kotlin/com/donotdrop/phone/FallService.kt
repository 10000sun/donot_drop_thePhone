package com.donotdrop.phone

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/** 포그라운드 서비스. 언제 센서를 읽을지는 MotionGate가 정한다. */
class FallService : Service() {
    private var gate: MotionGate? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "낙하 감시", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("낙하 감시 중")
            .setContentText("폰이 떨어지면 비명을 지릅니다")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(this, 1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        val app = application as App
        gate = MotionGate(this, app.monitor, app.settings.keepAwake).also { it.start() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        gate?.stop()
        super.onDestroy()
    }

    private companion object { const val CHANNEL = "monitor" }
}

/** 감시를 켜거나 끈다(설정에 기억해서 재부팅 후에도 따른다). */
fun setMonitoring(context: Context, on: Boolean) {
    (context.applicationContext as App).settings.monitorEnabled = on
    val i = Intent(context, FallService::class.java)
    if (on) context.startForegroundService(i) else context.stopService(i)
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val s = (context.applicationContext as App).settings
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && s.onboarded && s.monitorEnabled) setMonitoring(context, true)
    }
}
