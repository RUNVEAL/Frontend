plugins {
    id("runveal.android.library.compose")
}

android {
    namespace = "com.swyp.runveal.feature.home"
}

dependencies {
    implementation(project(":androidApp:core:designsystem"))
    implementation(project(":androidApp:core:map"))
    implementation(project(":androidApp:core:navigation"))
    implementation(project(":androidApp:core:permission"))

    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.compose.material3)
}
