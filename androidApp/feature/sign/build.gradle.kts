plugins {
    id("runveal.android.feature")
}

android {
    namespace = "com.swyp.runveal.feature.sign"
}

dependencies {
    implementation(project(":androidApp:core:designsystem"))
    implementation(project(":androidApp:core:navigation"))
    implementation(project(":sharedLogic"))

    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.compose.material3)
}
