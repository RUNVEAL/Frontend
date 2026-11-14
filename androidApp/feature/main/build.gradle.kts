plugins {
    id("runveal.android.library.compose")
}

// design/images/* → Android drawable-xxxhdpi 리소스로 복사
apply(from = rootProject.file("gradle/runveal-images.gradle.kts"))

tasks.named("preBuild") {
    dependsOn("generateRunvealImages")
}

android {
    sourceSets["main"].res.srcDir(layout.buildDirectory.dir("generated/runvealImages/res").get())

    namespace = "com.swyp.runveal.feature.main"
}

dependencies {
    implementation(project(":androidApp:core:designsystem"))
    implementation(project(":androidApp:core:navigation"))
    implementation(project(":androidApp:feature:home"))
    implementation(project(":sharedLogic"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.compose.material3)
}
