plugins {
    id("runveal.android.library.compose")
}

android {
    namespace = "com.swyp.runveal.core.devicestate"
}

dependencies {
    implementation(project(":androidApp:core:designsystem"))

    implementation(libs.compose.material3)
}
