package com.swyp.runveal.tabmypage.contract

/**
 * 오픈소스 라이선스 고지 한 줄 — 라이브러리 이름과 라이선스.
 *
 * 이름과 라이선스 이름은 고유명사라 번역하지 않는다(그래서 string resource가 아니다).
 *
 * TODO: 지금은 `gradle/libs.versions.toml`을 보고 **손으로** 적어 둔 목록이라, 의존성이 바뀌면
 *  같이 고쳐야 하고 빠뜨리기도 쉽다. 출시 전에 라이선스 리포트를 빌드에서 자동 생성하도록
 *  바꾼다(예: `com.mikepenz.aboutlibraries.plugin`) — 그래야 추이 의존성까지 빠짐없이 모인다.
 */
internal data class OpenSourceLicense(val name: String, val license: String)

/**
 * 앱이 쓰는 오픈소스. 직접 선언한 의존성만 적었다 — 추이 의존성까지 포함한 완전한 목록은 위 TODO의
 * 자동 생성으로 대체한다.
 */
internal val OPEN_SOURCE_LICENSES: List<OpenSourceLicense> = listOf(
    OpenSourceLicense("MapLibre GL Native", "BSD 2-Clause"),
    OpenSourceLicense("Kotlin", "Apache License 2.0"),
    OpenSourceLicense("kotlinx.coroutines", "Apache License 2.0"),
    OpenSourceLicense("kotlinx.serialization", "Apache License 2.0"),
    OpenSourceLicense("Compose Multiplatform", "Apache License 2.0"),
    OpenSourceLicense("AndroidX (Core, Lifecycle, Navigation, Security, Credentials)", "Apache License 2.0"),
    OpenSourceLicense("AndroidX Health Connect", "Apache License 2.0"),
    OpenSourceLicense("Ktor", "Apache License 2.0"),
    OpenSourceLicense("OkHttp", "Apache License 2.0"),
    OpenSourceLicense("SQLDelight", "Apache License 2.0"),
    OpenSourceLicense("Lottie for Android", "Apache License 2.0"),
    OpenSourceLicense("Kakao SDK for Android", "Apache License 2.0"),
)
