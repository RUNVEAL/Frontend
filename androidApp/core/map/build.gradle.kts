plugins {
    id("runveal.android.library.compose")
    id("runveal.kotlin.serialization")
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
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
}
