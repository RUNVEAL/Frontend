plugins {
    id("runveal.android.library.compose")
}

android {
    namespace = "com.sloth.runveal.core.uicore"
}

dependencies {
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.kotlinx.coroutines.core)
}
