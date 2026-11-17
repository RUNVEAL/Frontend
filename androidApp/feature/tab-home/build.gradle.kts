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

    namespace = "com.swyp.runveal.feature.tabhome"
}

dependencies {
    implementation(project(":androidApp:core:designsystem"))
    implementation(project(":androidApp:core:devicestate"))
    implementation(project(":androidApp:core:map"))
    implementation(project(":androidApp:core:navigation"))
    implementation(project(":sharedLogic"))

    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.material3)
}
