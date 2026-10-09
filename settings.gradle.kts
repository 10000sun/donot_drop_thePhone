pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
    plugins {
        id("com.android.application") version "8.7.3"
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
// shortcut: app 모듈은 Android SDK가 있을 때만 포함(SDK 없는 CI/샌드박스에서 core 테스트용), SDK 있는 환경에선 항상 포함됨
if (System.getenv("ANDROID_HOME") != null || file("local.properties").exists()) include(":app")
