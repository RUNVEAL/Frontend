plugins {
    id("runveal.android.library.compose")
}

android {
    namespace = "com.swyp.runveal.core.uicore"
}

dependencies {
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.kotlinx.coroutines.core)
}
