plugins {
    id("runveal.android.library.compose")
}

android {
    namespace = "com.swyp.runveal.core.ui"
}

dependencies {
    implementation(project(":androidApp:core:designsystem"))
    // 화면이 보여줄지 말지를 판단하는 기기 상태(연결 여부 등).
    implementation(project(":androidApp:core:devicestate"))

    // BackHandler — 오버레이가 떠 있는 동안 시스템 뒤로가기를 가로챈다.
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.material3)
}
