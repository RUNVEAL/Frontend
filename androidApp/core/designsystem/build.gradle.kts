import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
}

// design/runveal-tokens.json → Kotlin 디자인 토큰 자동 생성
apply(from = rootProject.file("gradle/runveal-tokens.gradle.kts"))
// design/icons/*.svg → Android Vector Drawable + RunvealIcons.kt 자동 생성
apply(from = rootProject.file("gradle/runveal-icons.gradle.kts"))

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

tasks.named("preBuild") {
    dependsOn("generateRunvealTokens", "generateRunvealIcons")
}

dependencies {
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    sourceSets["main"].kotlin.srcDir(layout.buildDirectory.dir("generated/runvealTokens/commonMain/kotlin").get())
    sourceSets["main"].kotlin.srcDir(layout.buildDirectory.dir("generated/runvealIcons/kotlin").get())
    sourceSets["main"].res.srcDir(layout.buildDirectory.dir("generated/runvealIcons/res").get())

    namespace = "com.swyp.runveal.designsystem"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}
