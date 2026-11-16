plugins {
    id("runveal.android.feature")
}

// design/images/* → Android drawable-xxxhdpi 리소스로 복사
apply(from = rootProject.file("gradle/runveal-images.gradle.kts"))

tasks.named("preBuild") {
    dependsOn("generateRunvealImages")
}

android {
    sourceSets["main"].res.srcDir(layout.buildDirectory.dir("generated/runvealImages/res").get())

    namespace = "com.swyp.runveal.feature.tabmypage"
}

dependencies {
    implementation(project(":androidApp:core:designsystem"))
    implementation(project(":androidApp:core:health"))
    // 결과 화면의 경로 지도 — CourseSnapshotImage
    implementation(project(":androidApp:core:map"))
    implementation(project(":androidApp:core:navigation"))
    implementation(project(":androidApp:core:permission"))
    implementation(project(":sharedLogic"))

    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.material3)
    // Health Connect 권한은 OS 다이얼로그가 아니라 Health Connect 앱 화면에서 받는다 —
    // 그 요청 계약(PermissionController.createRequestPermissionResultContract)을 화면이 직접 띄운다.
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.health.connect.client)
}
