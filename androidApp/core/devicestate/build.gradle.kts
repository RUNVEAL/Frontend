plugins {
    id("runveal.android.library.compose")
}

android {
    namespace = "com.sloth.runveal.core.devicestate"
}

dependencies {
    // 상태만 다루는 모듈이라 UI 의존성(designsystem 등)은 두지 않는다 — 화면은 core:ui 몫이다.
    implementation(project(":sharedLogic"))

    implementation(libs.androidx.core.ktx)
    // 위치 서비스 "켜기" 확인 다이얼로그(SettingsClient).
    implementation(libs.play.services.location)
    // rememberLauncherForActivityResult — 그 다이얼로그를 띄우는 데 쓴다.
    implementation(libs.androidx.activity.compose)
}
