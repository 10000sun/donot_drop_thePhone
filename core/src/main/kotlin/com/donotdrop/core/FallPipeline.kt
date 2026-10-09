package com.donotdrop.core

enum class Outcome { CATCH, IMPACT, UNSURE }

sealed interface PipelineEvent {
    data object Started : PipelineEvent
    /** 낙하 종료 즉시(착지 판정 전). 비명을 바로 끊는 용도. */
    data object Ended : PipelineEvent
    data class Landed(
        val durationSec: Double,
        val heightM: Double,
        val peak: Float,
        val outcome: Outcome,
    ) : PipelineEvent
}

/**
 * 낙하 감지 + 종료 직후 windowNs 동안의 최대 가속도로 잡음/충돌을 구분한다.
 * 충돌은 짧고 매우 큰 스파이크(peak >= impactMin), 손으로 받으면 더 낮다(peak <= catchMax), 사이는 UNSURE(중립 대사).
 * shortcut: 피크 크기만 사용, 지속 시간 패턴은 안 봄. 보정 모드 실험 결과로 부족하면 윈도우 구간의 에너지/길이도 사용.
 */
class FallPipeline(
    freeFallMs2: Float = 2.5f,
    minFallNs: Long = 50_000_000L,
    private val catchMax: Float = 15f,
    private val impactMin: Float = 30f,
    private val windowNs: Long = 100_000_000L,
) {
    private val detector = FallDetector(freeFallMs2, minFallNs)
    private var landed: FallEvent.Ended? = null
    private var windowStart = 0L
    private var peak = 0f

    fun onSample(timestampNs: Long, magnitude: Float): PipelineEvent? {
        val ended = landed
        if (ended != null) {
            peak = maxOf(peak, magnitude)
            if (timestampNs - windowStart < windowNs) return null
            landed = null
            val outcome = when {
                peak >= impactMin -> Outcome.IMPACT
                peak <= catchMax -> Outcome.CATCH
                else -> Outcome.UNSURE
            }
            return PipelineEvent.Landed(ended.durationSec, ended.heightM, peak, outcome)
        }
        return when (val e = detector.onSample(timestampNs, magnitude)) {
            FallEvent.Started -> PipelineEvent.Started
            is FallEvent.Ended -> { landed = e; windowStart = timestampNs; peak = magnitude; PipelineEvent.Ended }
            null -> null
        }
    }
}
