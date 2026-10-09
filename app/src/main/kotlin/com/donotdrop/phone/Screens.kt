package com.donotdrop.phone

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.donotdrop.core.Outcome

private val Outcome.label get() = when (this) {
    Outcome.CATCH -> "잡음"
    Outcome.IMPACT -> "충돌"
    Outcome.UNSURE -> "애매함"
}

@Composable
fun MainScreen(m: FallMonitor, onCalibrate: () -> Unit) = Column(Modifier.padding(16.dp), Arrangement.spacedBy(12.dp)) {
    Text("낙하 비명 폰", style = MaterialTheme.typography.headlineMedium)
    if (m.sensorMissing) Text("이 기기에는 가속도 센서가 없습니다.")
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text("감시", Modifier.weight(1f))
        Switch(m.running, { if (it) m.start() else m.stop() }, enabled = !m.sensorMissing)
    }
    val l = m.last
    Text(if (l == null) "아직 낙하 기록이 없습니다." else
        "마지막 낙하: %.2f초, 약 %.0fcm (%s)".format(l.durationSec, l.heightM * 100, l.outcome.label))
    Button(onCalibrate) { Text("보정 모드") }
}

@Composable
fun CalibrationScreen(m: FallMonitor, onBack: () -> Unit) = Column(Modifier.padding(16.dp), Arrangement.spacedBy(8.dp)) {
    Text("보정 모드", style = MaterialTheme.typography.headlineMedium)
    Text("반드시 침대·쿠션 위에서 테스트하세요. 폰을 일부러 던지지 마세요.", color = MaterialTheme.colorScheme.error)
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text("감시", Modifier.weight(1f))
        Switch(m.running, { if (it) m.start() else m.stop() }, enabled = !m.sensorMissing)
    }
    Text("가속도 크기: %.1f m/s²".format(m.magnitude))
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
        m.graphTick // 값이 바뀔 때마다 다시 그리도록 구독
        val n = m.graph.size
        val path = Path()
        for (i in 0 until n) {
            val v = m.graph[(m.graphIndex + i) % n]
            val x = size.width * i / (n - 1)
            val y = size.height * (1f - (v / 60f).coerceIn(0f, 1f))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(2f))
    }
    m.last?.let { Text("마지막: %.2f초, 착지 피크 %.1f m/s² → %s".format(it.durationSec, it.peak, it.outcome.label)) }
    Slider1("낙하 기준 %.1f m/s² 미만".format(m.freeFall), m.freeFall, 1f..5f, { m.freeFall = it }, m::rebuild)
    Slider1("잡음 판정 피크 ≤ %.0f".format(m.catchMax), m.catchMax, 5f..40f, { m.catchMax = it }, m::rebuild)
    Slider1("충돌 판정 피크 ≥ %.0f".format(m.impactMin), m.impactMin, 15f..80f, { m.impactMin = it }, m::rebuild)
    Button(onBack) { Text("돌아가기") }
}

@Composable
private fun Slider1(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit, onDone: () -> Unit) {
    Text(label)
    Slider(value, onChange, valueRange = range, onValueChangeFinished = onDone)
}
