package com.donotdrop.phone

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun MainScreen(m: FallMonitor, onCalibrate: () -> Unit, onSettings: () -> Unit, onHistory: () -> Unit) = Column(Modifier.padding(16.dp), Arrangement.spacedBy(12.dp)) {
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
    Button(onSettings) { Text("설정") }
    Button(onHistory) { Text("낙하 기록") }
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

@Composable
fun OnboardingScreen(onOk: () -> Unit) = Column(Modifier.padding(16.dp), Arrangement.spacedBy(16.dp)) {
    Text("먼저 안전 안내", style = MaterialTheme.typography.headlineMedium)
    Text("폰을 일부러 던지거나 떨어뜨리지 마세요. 폰이 파손될 수 있습니다.\n테스트와 보정은 반드시 침대나 쿠션 위에서 하세요.")
    Button(onOk) { Text("확인했어요") }
}

@Composable
fun SettingsScreen(s: Settings, m: FallMonitor, onBack: () -> Unit) = Column(Modifier.padding(16.dp), Arrangement.spacedBy(8.dp)) {
    Text("설정", style = MaterialTheme.typography.headlineMedium)
    var catchLine by remember { mutableStateOf(s.catchLine) }
    var impactLine by remember { mutableStateOf(s.impactLine) }
    var unsureLine by remember { mutableStateOf(s.unsureLine) }
    var volume by remember { mutableStateOf(s.volume) }
    val bounds = remember { mutableStateListOf(*s.bounds.map { it.toFloat() }.toTypedArray()) }
    OutlinedTextField(catchLine, { catchLine = it }, label = { Text("잡았을 때 대사") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(impactLine, { impactLine = it }, label = { Text("충돌 대사") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(unsureLine, { unsureLine = it }, label = { Text("애매할 때 대사") }, modifier = Modifier.fillMaxWidth())
    Text("비명 볼륨")
    Slider(volume, { volume = it }, valueRange = 0f..1f)
    Text("반응 구간 경계(초)")
    bounds.forEachIndexed { i, v ->
        Text("%.2f초".format(v))
        Slider(v, { bounds[i] = it }, valueRange = 0.05f..1f)
    }
    Button({
        s.catchLine = catchLine; s.impactLine = impactLine; s.unsureLine = unsureLine; s.volume = volume
        s.bounds = bounds.map { it.toDouble() }.sorted() // 경계는 항상 오름차순
        s.applyTo(m)
        onBack()
    }) { Text("저장") }
}

@Composable
fun HistoryScreen(h: HistoryStore, onBack: () -> Unit) = Column(Modifier.padding(16.dp), Arrangement.spacedBy(8.dp)) {
    Text("낙하 기록", style = MaterialTheme.typography.headlineMedium)
    val fmt = remember { java.text.SimpleDateFormat("M/d HH:mm:ss", java.util.Locale.KOREA) }
    val records = remember { h.all() }
    if (records.isEmpty()) Text("기록이 없습니다.")
    androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f)) {
        items(records.size) { i ->
            val r = records[i]
            Text("${fmt.format(java.util.Date(r.time))}  %.2f초 · 약 %.0fcm · %s".format(r.durationSec, r.heightM * 100, r.outcome.label))
        }
    }
    Button(onBack) { Text("돌아가기") }
}
