package com.donotdrop.core

/** 낙하가 이어진 시간(초)에 따른 비명 재생 속도. SoundPool 허용 범위(0.5~2.0) 안에서 올라간다. */
object ScreamPlan {
    fun rate(elapsedSec: Double): Float = (1.0 + elapsedSec * 1.5).coerceIn(1.0, 2.0).toFloat()
}
