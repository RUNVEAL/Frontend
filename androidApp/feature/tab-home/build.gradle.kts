plugins {
    id("runveal.android.library.compose")
}

android {
    namespace = "com.swyp.runveal.feature.tabhome"
}

dependencies {
    implementation(project(":androidApp:core:map"))
    implementation(project(":androidApp:core:navigation"))
    implementation(project(":androidApp:core:permission"))

    implementation(libs.androidx.navigation.compose)
}
