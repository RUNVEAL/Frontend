plugins {
    id("runveal.android.library.compose")
}

android {
    namespace = "com.swyp.runveal.feature.tabmypage"
}

dependencies {
    implementation(project(":androidApp:core:designsystem"))
    implementation(project(":androidApp:core:navigation"))

    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.material3)
}
