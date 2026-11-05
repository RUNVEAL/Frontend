plugins {
    id("runveal.android.library.compose")
}

android {
    namespace = "com.swyp.runveal.feature.home"
}

dependencies {
    implementation(project(":androidApp:core:designsystem"))
    implementation(project(":androidApp:core:navigation"))
    implementation(project(":androidApp:feature:tab-course"))
    implementation(project(":androidApp:feature:tab-home"))
    implementation(project(":androidApp:feature:tab-mypage"))
    implementation(project(":androidApp:feature:tab-record"))

    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.core)
}
