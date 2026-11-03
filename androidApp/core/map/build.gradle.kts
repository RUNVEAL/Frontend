plugins {
    id("runveal.android.library.compose")
}

android {
    namespace = "com.swyp.runveal.core.map"
}

dependencies {
    implementation(project(":sharedLogic"))
    implementation(project(":androidApp:core:designsystem"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.maplibre.android.sdk)
    implementation(libs.kotlinx.serialization.json)
}
