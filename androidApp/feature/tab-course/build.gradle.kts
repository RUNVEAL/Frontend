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

    namespace = "com.sloth.runveal.feature.tabcourse"
}

dependencies {
    implementation(project(":androidApp:core:designsystem"))
    implementation(project(":androidApp:core:navigation"))
    implementation(project(":androidApp:core:map"))
    implementation(project(":sharedLogic"))
    // 내 코스에서 러닝 시작 이벤트(course_start) — Firebase는 core:analytics만 안다.
    implementation(project(":androidApp:core:analytics"))

    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.serialization.json)
}
