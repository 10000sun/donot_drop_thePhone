package com.donotdrop.core

enum class SamplingMode {
    /** 화면 꺼짐 + 정지: 고속 샘플링과 웨이크락 없음. 기기의 '움직임 감지' 트리거만 걸어 둔다. */
    IDLE,
    /** 화면이 켜져 있음: CPU가 이미 깨어 있으니 웨이크락 없이 고속 샘플링. */
    SCREEN,
    /** 화면은 꺼졌지만 폰이 움직이는 중: 고속 샘플링 + 짧은 웨이크락, '정지 감지' 트리거로 끝낸다. */
    MOTION,
}

/** 화면/움직임/정지 이벤트로 샘플링 모드를 정한다. 안드로이드 API와 분리해서 테스트한다. */
class SamplingGate {
    var mode = SamplingMode.IDLE
        private set

    fun screenOn() = set(SamplingMode.SCREEN)
    fun screenOff() = set(SamplingMode.IDLE)
    fun motion() = set(if (mode == SamplingMode.IDLE) SamplingMode.MOTION else mode)
    fun still() = set(if (mode == SamplingMode.MOTION) SamplingMode.IDLE else mode)

    private fun set(m: SamplingMode): SamplingMode { mode = m; return m }
}
