plugins {
    id("runveal.android.feature")
}

android {
    namespace = "com.swyp.runveal.feature.courserecommend"
}

dependencies {
    implementation(project(":androidApp:core:map"))
    implementation(project(":androidApp:core:designsystem"))
    implementation(project(":androidApp:core:navigation"))
    implementation(project(":sharedLogic"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.material3)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.serialization.json)
}
