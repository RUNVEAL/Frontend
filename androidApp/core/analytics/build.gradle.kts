plugins {
    id("runveal.android.library")
}

android {
    namespace = "com.sloth.runveal.core.analytics"
}

dependencies {
    // 이벤트 이름 상수(RunvealAnalytics) — iOS와 같은 문자열을 쓰려고 sharedLogic에 둔다.
    implementation(project(":sharedLogic"))

    // Firebase 의존성을 이 모듈에만 둔다. feature가 직접 SDK를 들고 있으면 이벤트를 추가할
    // 화면마다 의존성이 늘고, 나중에 수집 도구를 바꿀 때 전부 고쳐야 한다.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
}
