plugins {
    id("runveal.android.library.compose")
}

android {
    namespace = "com.swyp.runveal.core.devicestate"
}

dependencies {
    // 상태만 다루는 모듈이라 UI 의존성(designsystem 등)은 두지 않는다 — 화면은 core:ui 몫이다.
}
