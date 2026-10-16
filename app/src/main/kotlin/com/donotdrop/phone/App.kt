package com.donotdrop.phone

import android.app.Application

/** 서비스와 화면이 같은 모니터·설정·기록을 공유한다. */
class App : Application() {
    val settings by lazy { Settings(this) }
    val history by lazy { HistoryStore(this) }
    val monitor by lazy { FallMonitor(this).also { settings.applyTo(it); it.onLanded = history::add } }
}
