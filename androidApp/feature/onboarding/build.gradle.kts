plugins {
    id("runveal.android.feature")
}

android {
    namespace = "com.sloth.runveal.feature.onboarding"
}

dependencies {
    implementation(project(":androidApp:core:designsystem"))
    implementation(project(":androidApp:core:navigation"))
    implementation(project(":sharedLogic"))

    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.material3)
}
