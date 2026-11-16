plugins {
    id("runveal.android.library.compose")
}

android {
    namespace = "com.swyp.runveal.core.devicestate"
}

dependencies {
    implementation(project(":androidApp:core:designsystem"))

    // BackHandler — 오버레이가 떠 있는 동안 시스템 뒤로가기를 가로채 닫는다.
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.material3)
}
