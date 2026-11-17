plugins {
    id("runveal.android.library")
}

android {
    namespace = "com.sloth.runveal.core.health"
}

dependencies {
    implementation(project(":sharedLogic"))
    // 가져온 기록도 직접 기록한 러닝과 같은 경로로 집계하므로 구간 인덱스 들여오기를 그대로 쓴다.
    implementation(project(":androidApp:core:map"))

    implementation(libs.androidx.health.connect.client)
    implementation(libs.kotlinx.coroutines.core)
}
