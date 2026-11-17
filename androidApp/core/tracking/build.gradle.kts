plugins {
    id("runveal.android.library")
}

android {
    namespace = "com.sloth.runveal.core.tracking"
}

dependencies {
    implementation(project(":sharedLogic"))
    implementation(project(":androidApp:core:map"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
