plugins {
    id("runveal.android.library.compose")
}

android {
    namespace = "com.swyp.runveal.feature.main"
}

dependencies {
    implementation(project(":androidApp:core:designsystem"))
    implementation(project(":androidApp:core:navigation"))
    implementation(project(":androidApp:core:network"))
    implementation(project(":androidApp:feature:home"))
    implementation(project(":sharedLogic"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.compose.material3)
}
