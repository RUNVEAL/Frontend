plugins {
    id("runveal.android.library.compose")
    id("runveal.kotlin.serialization")
}

android {
    namespace = "com.swyp.runveal.core.navigation"
}

dependencies {
    implementation(libs.androidx.navigation.compose)
}
