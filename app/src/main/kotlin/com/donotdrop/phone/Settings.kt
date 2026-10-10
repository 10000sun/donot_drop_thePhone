package com.donotdrop.phone

import android.content.Context
import com.donotdrop.core.Outcome
import com.donotdrop.core.PipelineEvent
import org.json.JSONArray
import org.json.JSONObject

/** 설정은 SharedPreferences에 저장한다. 센서 원본 값은 저장하지 않는다. */
class Settings(context: Context) {
    private val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var onboarded get() = p.getBoolean("onboarded", false); set(v) = p.edit().putBoolean("onboarded", v).apply()
    var monitorEnabled get() = p.getBoolean("monitorEnabled", true); set(v) = p.edit().putBoolean("monitorEnabled", v).apply()
    /** 화면이 꺼져도 감시하려면 웨이크락이 필요하다. Play 배터리 지표(과도한 부분 웨이크락) 때문에 기본은 끔. */
    var keepAwake get() = p.getBoolean("keepAwake", false); set(v) = p.edit().putBoolean("keepAwake", v).apply()
    var volume get() = p.getFloat("volume", 1f); set(v) = p.edit().putFloat("volume", v).apply()
    var catchLine get() = p.getString("catchLine", null) ?: "휴 깜짝 놀랐잖아요, 다음부턴 조심해주세요"
        set(v) = p.edit().putString("catchLine", v).apply()
    var impactLine get() = p.getString("impactLine", null) ?: "아야!"; set(v) = p.edit().putString("impactLine", v).apply()
    var unsureLine get() = p.getString("unsureLine", null) ?: "어, 방금 뭐였죠?"; set(v) = p.edit().putString("unsureLine", v).apply()
    var bounds: List<Double>
        get() = listOf(0.15, 0.30, 0.50).mapIndexed { i, d -> p.getFloat("bound$i", d.toFloat()).toDouble() }
        set(v) = p.edit().apply { v.forEachIndexed { i, d -> putFloat("bound$i", d.toFloat()) } }.apply()

    fun applyTo(m: FallMonitor) {
        m.freeFall = p.getFloat("freeFall", 2.5f)
        m.catchMax = p.getFloat("catchMax", 15f)
        m.impactMin = p.getFloat("impactMin", 30f)
        m.volume = volume
        m.catchLine = catchLine; m.impactLine = impactLine; m.unsureLine = unsureLine
        m.bounds = bounds
    }

    fun saveThresholds(m: FallMonitor) =
        p.edit().putFloat("freeFall", m.freeFall).putFloat("catchMax", m.catchMax).putFloat("impactMin", m.impactMin).apply()
}

data class FallRecord(val time: Long, val durationSec: Double, val heightM: Double, val outcome: Outcome)

/** 기록에는 낙하 시간·추정 높이·결과만 저장하고 최근 50건만 유지한다. */
class HistoryStore(context: Context) {
    private val p = context.getSharedPreferences("history", Context.MODE_PRIVATE)

    fun all(): List<FallRecord> {
        val a = JSONArray(p.getString("items", "[]"))
        return (0 until a.length()).map {
            a.getJSONObject(it).run { FallRecord(getLong("t"), getDouble("d"), getDouble("h"), Outcome.valueOf(getString("o"))) }
        }
    }

    fun add(e: PipelineEvent.Landed) {
        val a = JSONArray()
        (listOf(FallRecord(System.currentTimeMillis(), e.durationSec, e.heightM, e.outcome)) + all()).take(50).forEach {
            a.put(JSONObject().put("t", it.time).put("d", it.durationSec).put("h", it.heightM).put("o", it.outcome.name))
        }
        p.edit().putString("items", a.toString()).apply()
    }
}
