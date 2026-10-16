package com.donotdrop.phone

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * 광고 영역. 광고 코드는 이 파일에서만 다룬다(배치는 MainActivity의 AdSlot() 호출 한 곳).
 * 메인/기록 화면 하단에만 두고, 안전 안내·보정·설정 화면에는 두지 않는다(스위치·슬라이더 근처 오클릭 방지).
 * TODO: AdMob 연동 시 여기서 320x50 배너를 그린다(동의 UMP 포함). 지금은 디버그 빌드에서만 자리를 표시한다.
 */
@Composable
fun AdSlot() {
    val debuggable = LocalContext.current.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    if (!debuggable) return
    Box(
        Modifier.fillMaxWidth().height(50.dp).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) { Text("광고 영역 (디버그 표시)") }
}
