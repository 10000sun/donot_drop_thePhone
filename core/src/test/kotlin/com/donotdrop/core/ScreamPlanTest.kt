package com.donotdrop.core

import kotlin.test.*

class ScreamPlanTest {
    @Test fun `시간이 길수록 빨라지고 2배를 넘지 않는다`() {
        assertEquals(1f, ScreamPlan.rate(0.0))
        assertTrue(ScreamPlan.rate(0.3) > ScreamPlan.rate(0.1))
        assertEquals(2f, ScreamPlan.rate(5.0))
    }
}
