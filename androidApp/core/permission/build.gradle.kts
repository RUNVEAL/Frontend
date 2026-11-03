plugins {
    id("runveal.android.library.compose")
}

android {
    namespace = "com.swyp.runveal.core.permission"
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.material3)
}
