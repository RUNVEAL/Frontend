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

    namespace = "com.sloth.runveal.feature.running"
}

dependencies {
    implementation(project(":sharedLogic"))
    // 러닝 기록 화면 진입 이벤트(run_start) — Firebase는 이 모듈이 아니라 core:analytics가 안다.
    implementation(project(":androidApp:core:analytics"))
    implementation(project(":androidApp:core:designsystem"))
    implementation(project(":androidApp:core:map"))
    implementation(project(":androidApp:core:navigation"))
    // 러닝 시작 시점에 알림 권한을 묻는다(RequestNotificationPermissionOnce).
    implementation(project(":androidApp:core:permission"))
    implementation(project(":androidApp:core:tracking"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.material3)
}
