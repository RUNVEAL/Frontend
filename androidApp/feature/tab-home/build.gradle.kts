plugins {
    id("runveal.android.library.compose")
}

android {
    namespace = "com.swyp.runveal.feature.tabhome"
}

dependencies {
    implementation(project(":androidApp:core:designsystem"))
    implementation(project(":androidApp:core:map"))
    implementation(project(":androidApp:core:navigation"))
    implementation(project(":sharedLogic"))

    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.material3)
}
