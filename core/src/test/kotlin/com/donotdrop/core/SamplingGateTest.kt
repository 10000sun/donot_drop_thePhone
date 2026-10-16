package com.donotdrop.core

import kotlin.test.*

class SamplingGateTest {
    @Test fun `꺼진 화면에서 움직이면 활성, 정지하면 다시 대기`() {
        val g = SamplingGate()
        assertEquals(SamplingMode.MOTION, g.motion())
        assertEquals(SamplingMode.IDLE, g.still())
    }

    @Test fun `화면이 켜져 있으면 정지 신호로 끄지 않는다`() {
        val g = SamplingGate()
        g.motion(); g.screenOn()
        assertEquals(SamplingMode.SCREEN, g.still())
        assertEquals(SamplingMode.SCREEN, g.motion())
    }

    @Test fun `화면이 꺼지면 대기로 돌아간다`() {
        val g = SamplingGate()
        g.screenOn()
        assertEquals(SamplingMode.IDLE, g.screenOff())
    }
}
