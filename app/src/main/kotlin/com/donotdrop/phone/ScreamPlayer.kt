package com.donotdrop.phone

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.donotdrop.core.ScreamPlan

/** 낙하 감지 즉시 재생하려고 미리 로드해 둔다. 음원은 임시 테스트 파일(res/raw/scream_test.wav). */
class ScreamPlayer(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
        .build()
    private val soundId = pool.load(context, R.raw.scream_test, 1)
    private var streamId = 0
    var volume = 1f

    fun start() {
        streamId = pool.play(soundId, volume, volume, 1, -1, 1f)
    }

    fun update(elapsedSec: Double) {
        if (streamId != 0) pool.setRate(streamId, ScreamPlan.rate(elapsedSec))
    }

    fun stop() {
        if (streamId != 0) pool.stop(streamId)
        streamId = 0
    }

    fun release() = pool.release()
}
