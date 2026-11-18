plugins {
    id("runveal.android.feature")
}

android {
    namespace = "com.sloth.runveal.feature.home"
}

dependencies {
    implementation(project(":androidApp:core:designsystem"))
    implementation(project(":androidApp:core:devicestate"))
    implementation(project(":androidApp:core:map"))
    implementation(project(":androidApp:core:navigation"))
    implementation(project(":androidApp:core:permission"))
    implementation(project(":androidApp:feature:course-recommend"))
    implementation(project(":androidApp:feature:running"))
    implementation(project(":androidApp:feature:tab-course"))
    implementation(project(":androidApp:feature:tab-home"))
    implementation(project(":androidApp:feature:tab-mypage"))
    implementation(project(":androidApp:feature:tab-record"))
    implementation(project(":sharedLogic"))

    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.core)
    // 러닝 시작 전 위치 안내 바텀시트(ModalBottomSheet)를 여기서 띄운다.
    implementation(libs.compose.material3)
}
