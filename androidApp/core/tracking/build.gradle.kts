plugins {
    id("runveal.android.library")
}

android {
    namespace = "com.sloth.runveal.core.tracking"
}

dependencies {
    implementation(project(":sharedLogic"))
    // 러닝 종료·저장 이벤트(run_complete) — Firebase는 core:analytics만 안다.
    implementation(project(":androidApp:core:analytics"))
    implementation(project(":androidApp:core:map"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
