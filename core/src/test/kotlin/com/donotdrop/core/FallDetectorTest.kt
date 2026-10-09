package com.donotdrop.core

import kotlin.test.*

class FallDetectorTest {
    private val ms = 1_000_000L

    private fun run(vararg segs: Pair<Float, Int>): List<FallEvent> {
        val d = FallDetector()
        var t = 0L
        val out = mutableListOf<FallEvent>()
        for ((mag, durMs) in segs) repeat(durMs / 5) { d.onSample(t, mag)?.let(out::add); t += 5 * ms }
        d.onSample(t, 9.8f)?.let(out::add) // 마지막에 정상 값으로 마무리
        return out
    }

    @Test fun `0_5초 낙하는 시작과 종료를 내고 높이는 약 1_2m`() {
        val e = run(9.8f to 100, 0.5f to 500)
        assertEquals(FallEvent.Started, e[0])
        val end = e[1] as FallEvent.Ended
        assertEquals(0.5, end.durationSec, 0.01)
        assertEquals(1.225, end.heightM, 0.05)
    }

    @Test fun `걷거나 흔드는 짧은 저가속은 낙하 아님`() {
        assertTrue(run(9.8f to 100, 0.5f to 40, 9.8f to 100).isEmpty())
    }

    @Test fun `정상 가속만 있으면 이벤트 없음`() {
        assertTrue(run(9.8f to 1000).isEmpty())
    }

    @Test fun `반응 구간 경계`() {
        assertEquals(Reaction.NONE, Fall.reaction(0.10))
        assertEquals(Reaction.SHORT, Fall.reaction(0.20))
        assertEquals(Reaction.MEDIUM, Fall.reaction(0.40))
        assertEquals(Reaction.LONG, Fall.reaction(0.60))
    }
}
