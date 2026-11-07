plugins {
    id("runveal.android.library.compose")
}

android {
    namespace = "com.swyp.runveal.core.permission"
}

dependencies {
    implementation(project(":androidApp:core:designsystem"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.material3)
    implementation(libs.play.services.location)
}
