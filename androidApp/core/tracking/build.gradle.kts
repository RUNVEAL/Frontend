plugins {
    id("runveal.android.library")
}

android {
    namespace = "com.swyp.runveal.core.tracking"
}

dependencies {
    implementation(project(":sharedLogic"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
