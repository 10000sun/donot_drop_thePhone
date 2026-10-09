package com.donotdrop.phone

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.donotdrop.core.Fall
import com.donotdrop.core.FallPipeline
import com.donotdrop.core.Outcome
import com.donotdrop.core.PipelineEvent
import com.donotdrop.core.Reaction
import java.util.Locale
import kotlin.math.sqrt

/** 가속도계 → 낙하 판정 → 비명/대사. 앱이 켜져 있는 동안만 동작한다(MVP). */
class FallMonitor(context: Context) : SensorEventListener {
    private val sm = context.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val scream = ScreamPlayer(context)
    private var tts: TextToSpeech? = null
    private var pipeline = FallPipeline()
    private var fallStartNs = 0L

    // 설정/보정 값. 바꾼 뒤 rebuild()를 부르면 반영된다.
    var freeFall by mutableFloatStateOf(2.5f)
    var catchMax by mutableFloatStateOf(15f)
    var impactMin by mutableFloatStateOf(30f)
    var bounds = listOf(0.15, 0.30, 0.50)
    var volume = 1f
    var catchLine = "휴 깜짝 놀랐잖아요, 다음부턴 조심해주세요"
    var impactLine = "아야!"
    var unsureLine = "어, 방금 뭐였죠?"
    var onLanded: ((PipelineEvent.Landed) -> Unit)? = null

    var running by mutableStateOf(false); private set
    var sensorMissing = sensor == null; private set
    var magnitude by mutableFloatStateOf(0f); private set
    var last by mutableStateOf<PipelineEvent.Landed?>(null); private set
    val graph = FloatArray(200)
    var graphIndex = 0; private set
    var graphTick by mutableIntStateOf(0); private set

    init {
        tts = TextToSpeech(context) { if (it == TextToSpeech.SUCCESS) tts?.language = Locale.KOREAN }
    }

    fun rebuild() { pipeline = FallPipeline(freeFall, catchMax = catchMax, impactMin = impactMin) }

    fun start() {
        val s = sensor ?: return
        rebuild()
        sm.registerListener(this, s, SensorManager.SENSOR_DELAY_FASTEST)
        running = true
    }

    fun stop() {
        sm.unregisterListener(this)
        scream.stop()
        running = false
    }

    fun release() {
        stop()
        scream.release()
        tts?.shutdown()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(e: SensorEvent) {
        val (x, y, z) = e.values
        val mag = sqrt(x * x + y * y + z * z)
        graph[graphIndex] = mag
        graphIndex = (graphIndex + 1) % graph.size
        if (graphIndex % 5 == 0) { magnitude = mag; graphTick++ } // 재구성 빈도 제한

        when (val ev = pipeline.onSample(e.timestamp, mag)) {
            PipelineEvent.Started -> { fallStartNs = e.timestamp; scream.volume = volume; scream.start() }
            PipelineEvent.Ended -> scream.stop()
            is PipelineEvent.Landed -> handleLanded(ev)
            null -> if (scream.playing) scream.update((e.timestamp - fallStartNs) / 1e9)
        }
    }

    private fun handleLanded(ev: PipelineEvent.Landed) {
        if (Fall.reaction(ev.durationSec, bounds) == Reaction.NONE) return // 손에서 살짝 흔들린 정도
        last = ev
        onLanded?.invoke(ev)
        val line = when (ev.outcome) {
            Outcome.CATCH -> catchLine
            Outcome.IMPACT -> impactLine
            Outcome.UNSURE -> unsureLine
        }
        tts?.speak(line, TextToSpeech.QUEUE_FLUSH, null, null)
    }
}
