plugins {
    id("runveal.android.library.compose")
}

android {
    namespace = "com.sloth.runveal.core.permission"
}

dependencies {
    implementation(project(":androidApp:core:designsystem"))
    // Health Connect 권한 판단(HealthConnectAvailability)과 권한 묶음(HealthConnectPermissions).
    implementation(project(":androidApp:core:health"))
    implementation(project(":sharedLogic"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.material3)
    // Health Connect 권한은 OS 다이얼로그가 아니라 Health Connect 앱 화면에서 받는다 —
    // 그 요청 계약(PermissionController.createRequestPermissionResultContract)을 여기서 띄운다.
    implementation(libs.androidx.health.connect.client)
}
