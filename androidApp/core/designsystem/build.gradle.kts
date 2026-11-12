plugins {
    id("runveal.android.library.compose")
}

// design/runveal-tokens.json → Kotlin 디자인 토큰 자동 생성
apply(from = rootProject.file("gradle/runveal-tokens.gradle.kts"))
// design/icons/*.svg → Android Vector Drawable + RunvealIcons.kt 자동 생성
apply(from = rootProject.file("gradle/runveal-icons.gradle.kts"))

tasks.named("preBuild") {
    dependsOn("generateRunvealTokens", "generateRunvealIcons")
}

dependencies {
    implementation(libs.compose.material3)
    implementation(libs.lottie.compose)
}

android {
    sourceSets["main"].kotlin.srcDir(layout.buildDirectory.dir("generated/runvealTokens/commonMain/kotlin").get())
    sourceSets["main"].kotlin.srcDir(layout.buildDirectory.dir("generated/runvealIcons/kotlin").get())
    sourceSets["main"].res.srcDir(layout.buildDirectory.dir("generated/runvealIcons/res").get())

    namespace = "com.swyp.runveal.designsystem"
}
