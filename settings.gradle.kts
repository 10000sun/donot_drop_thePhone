pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
    plugins {
        id("com.android.application") version "8.10.1"
        id("org.jetbrains.kotlin.android") version "2.0.21"
        id("org.jetbrains.kotlin.jvm") version "2.0.21"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
rootProject.name = "donot_drop_thePhone"
include(":core")
// app 모듈은 항상 포함한다(Android Studio는 local.properties/ANDROID_HOME이 동기화 전에 없을 수 있다).
// Android SDK가 없는 환경에서 core 테스트만 돌릴 때만 CORE_ONLY=1로 제외한다.
if (System.getenv("CORE_ONLY") == null) include(":app")
