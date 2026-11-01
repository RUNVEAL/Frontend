plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.swyp.runveal.core.permission"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
