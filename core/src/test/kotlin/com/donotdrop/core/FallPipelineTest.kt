package com.donotdrop.core

import kotlin.test.*

class FallPipelineTest {
    private fun run(landingPeak: Float): List<PipelineEvent> {
        val p = FallPipeline()
        val out = mutableListOf<PipelineEvent>()
        var t = 0L
        fun feed(mag: Float, ms: Int) = repeat(ms / 5) { p.onSample(t, mag)?.let(out::add); t += 5_000_000L }
        feed(9.8f, 100); feed(0.5f, 400)   // 0.4초 낙하
        feed(landingPeak, 5); feed(9.8f, 200) // 착지 스파이크 후 정상
        return out
    }

    @Test fun `큰 스파이크는 충돌`() {
        val landed = run(45f).last() as PipelineEvent.Landed
        assertEquals(Outcome.IMPACT, landed.outcome)
        assertEquals(0.4, landed.durationSec, 0.02)
    }

    @Test fun `작은 변화는 잡음`() = assertEquals(Outcome.CATCH, (run(12f).last() as PipelineEvent.Landed).outcome)

    @Test fun `중간은 애매`() = assertEquals(Outcome.UNSURE, (run(22f).last() as PipelineEvent.Landed).outcome)

    @Test fun `시작 이벤트가 먼저 나온다`() = assertEquals(PipelineEvent.Started, run(45f).first())
}
