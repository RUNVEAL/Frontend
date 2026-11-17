import Foundation

/**
 오픈소스 라이선스 고지 한 줄 — 라이브러리 이름과 라이선스.

 이름과 라이선스 이름은 고유명사라 번역하지 않는다(그래서 `MyStrings`에 없다).

 TODO: 지금은 SPM 패키지와 `gradle/libs.versions.toml`을 보고 **손으로** 적어 둔 목록이라,
  의존성이 바뀌면 같이 고쳐야 하고 빠뜨리기도 쉽다. 출시 전에 라이선스 목록을 빌드에서 자동
  생성하도록 바꾼다 — 그래야 추이 의존성까지 빠짐없이 모인다.
 */
struct OpenSourceLicense: Hashable {
    let name: String
    let license: String
}

/**
 iOS 앱이 쓰는 오픈소스. 직접 선언한 의존성만 적었다.

 **Android 목록을 그대로 쓰면 안 된다** — 플랫폼마다 들어가는 게 다르다. iOS에는 Compose·AndroidX·
 OkHttp·Kakao SDK·Health Connect가 없고(화면은 SwiftUI, 로그인은 1차 출시에서 꺼져 있다), 대신
 Ktor Darwin 엔진과 SQLDelight native 드라이버가 들어간다.
 */
let openSourceLicenses: [OpenSourceLicense] = [
    OpenSourceLicense(name: "MapLibre GL Native (iOS)", license: "BSD 2-Clause"),
    OpenSourceLicense(name: "Lottie for iOS", license: "Apache License 2.0"),
    OpenSourceLicense(name: "Kotlin", license: "Apache License 2.0"),
    OpenSourceLicense(name: "kotlinx.coroutines", license: "Apache License 2.0"),
    OpenSourceLicense(name: "kotlinx.serialization", license: "Apache License 2.0"),
    OpenSourceLicense(name: "Ktor", license: "Apache License 2.0"),
    OpenSourceLicense(name: "SQLDelight", license: "Apache License 2.0"),
]
