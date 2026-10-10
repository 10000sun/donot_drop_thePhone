package com.donotdrop.phone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.donotdrop.core.SamplingGate
import com.donotdrop.core.SamplingMode

/**
 * 배터리를 아끼려고 센서를 항상 고속으로 읽지 않는다.
 * - 화면 켜짐: 고속 샘플링(웨이크락 없음)
 * - 화면 꺼짐 + 폰이 움직이는 중: 고속 샘플링 + 웨이크락(최대 10분, '정지 감지'로 해제)
 * - 화면 꺼짐 + 정지: 샘플링 중단, 하드웨어 '움직임 감지' 트리거만 대기
 * 한계: 기기에 움직임/정지 감지 센서가 없으면 샘플링을 계속 켜 둔다(웨이크락 없이, 화면 꺼짐 중에는 불완전).
 * 움직임 감지는 약 5초 이상 움직여야 발동하므로, 가만히 놓인 폰을 툭 쳐서 떨어지는 경우는 놓칠 수 있다.
 * keepAwake가 켜져 있으면 예전처럼 항상 샘플링 + 웨이크락.
 */
class MotionGate(private val ctx: Context, private val monitor: FallMonitor, private val keepAwake: Boolean) {
    private val sm = ctx.getSystemService(SensorManager::class.java)
    private val pm = ctx.getSystemService(PowerManager::class.java)
    private val motionSensor: Sensor? = sm.getDefaultSensor(Sensor.TYPE_MOTION_DETECT)
    private val stillSensor: Sensor? = sm.getDefaultSensor(Sensor.TYPE_STATIONARY_DETECT)
    private val gate = SamplingGate()
    private var wake: PowerManager.WakeLock? = null

    private val onMotion = object : TriggerEventListener() {
        override fun onTrigger(e: TriggerEvent) = enter(gate.motion())
    }
    private val onStill = object : TriggerEventListener() {
        override fun onTrigger(e: TriggerEvent) = enter(gate.still())
    }
    private val screen = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) =
            enter(if (i.action == Intent.ACTION_SCREEN_ON) gate.screenOn() else gate.screenOff())
    }

    fun start() {
        if (keepAwake || motionSensor == null || stillSensor == null) {
            if (keepAwake) hold(null)
            monitor.start()
            return
        }
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON).apply { addAction(Intent.ACTION_SCREEN_OFF) }
        ContextCompat.registerReceiver(ctx, screen, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        enter(if (pm.isInteractive) gate.screenOn() else gate.screenOff())
    }

    fun stop() {
        if (motionSensor != null && stillSensor != null && !keepAwake) {
            runCatching { ctx.unregisterReceiver(screen) }
            sm.cancelTriggerSensor(onMotion, motionSensor)
            sm.cancelTriggerSensor(onStill, stillSensor)
        }
        release()
        monitor.stop()
    }

    private fun enter(mode: SamplingMode) {
        sm.cancelTriggerSensor(onMotion, motionSensor)
        sm.cancelTriggerSensor(onStill, stillSensor)
        when (mode) {
            SamplingMode.SCREEN -> { release(); monitor.start() }
            SamplingMode.MOTION -> { hold(10 * 60 * 1000L); monitor.start(); sm.requestTriggerSensor(onStill, stillSensor) }
            SamplingMode.IDLE -> { release(); monitor.stop(); sm.requestTriggerSensor(onMotion, motionSensor) }
        }
    }

    private fun hold(timeoutMs: Long?) {
        if (wake?.isHeld == true) return
        wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "donotdrop:monitor").apply {
            if (timeoutMs != null) acquire(timeoutMs) else acquire()
        }
    }

    private fun release() { wake?.takeIf { it.isHeld }?.release(); wake = null }
}
