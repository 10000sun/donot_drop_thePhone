package com.donotdrop.core

sealed interface FallEvent {
    /** 낙하로 판정된 시점(실제 낙하 시작은 minFallNs 만큼 앞). 이때 비명을 시작한다. */
    data object Started : FallEvent
    data class Ended(val durationSec: Double, val heightM: Double) : FallEvent
}

enum class Reaction { NONE, SHORT, MEDIUM, LONG }

object Fall {
    const val G = 9.8

    fun heightM(durationSec: Double) = 0.5 * G * durationSec * durationSec

    // 기획서 초기안: 0.15 / 0.30 / 0.50초 경계 (설정에서 바꿀 수 있도록 인자로 받음)
    fun reaction(durationSec: Double, bounds: List<Double> = listOf(0.15, 0.30, 0.50)): Reaction = when {
        durationSec < bounds[0] -> Reaction.NONE
        durationSec < bounds[1] -> Reaction.SHORT
        durationSec < bounds[2] -> Reaction.MEDIUM
        else -> Reaction.LONG
    }
}

/**
 * 가속도 크기(m/s²)가 thresholdMs2 미만인 상태가 minFallNs 이상 이어지면 낙하 시작,
 * 다시 thresholdMs2 이상이 되면 낙하 종료. 샘플은 시간순(timestampNs)으로 넣는다.
 */
class FallDetector(
    private val thresholdMs2: Float = 2.5f,
    private val minFallNs: Long = 50_000_000L,
) {
    private var lowSinceNs: Long? = null
    private var falling = false

    fun onSample(timestampNs: Long, magnitude: Float): FallEvent? {
        if (magnitude < thresholdMs2) {
            val since = lowSinceNs ?: timestampNs.also { lowSinceNs = it }
            if (!falling && timestampNs - since >= minFallNs) {
                falling = true
                return FallEvent.Started
            }
            return null
        }
        val since = lowSinceNs
        val wasFalling = falling
        lowSinceNs = null
        falling = false
        if (!wasFalling || since == null) return null
        val sec = (timestampNs - since) / 1e9
        return FallEvent.Ended(sec, Fall.heightM(sec))
    }
}
