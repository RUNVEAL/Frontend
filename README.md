This is a Kotlin Multiplatform project targeting Android, iOS. Android와 iOS는 각자 네이티브 UI(Android는
Compose, iOS는 SwiftUI)를 쓰고, `sharedLogic`만 두 플랫폼이 공유한다. 디자인 시스템은
[/design/runveal-tokens.json](./design/runveal-tokens.json)을 공통 소스로 두고 빌드 때 각 플랫폼 언어로
자동 생성한다 (Android는 `gradle/runveal-tokens.gradle.kts`, iOS는 `gradle/runveal-tokens-swift.gradle.kts`).

* [/androidApp](./androidApp/src) is the Android app. UI는 Jetpack Compose로 작성하고,
  [/androidApp/src/main/kotlin/com/sloth/runveal/designsystem](./androidApp/src/main/kotlin/com/sloth/runveal/designsystem)
  아래 디자인 시스템 코드(일부는 자동 생성)를 둔다.

* [/iosApp](./iosApp/iosApp) contains an iOS application. UI는 순수 SwiftUI로 작성하고,
  [/iosApp/iosApp/DesignSystem](./iosApp/iosApp/DesignSystem) 아래 디자인 시스템 코드(일부는 자동 생성)를 둔다.

* [/sharedLogic](./sharedLogic/src) is for the code that will be shared between app targets in the project.
  The most important subfolder is [commonMain](./sharedLogic/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :sharedLogic:testAndroidHostTest`
- iOS tests: `./gradlew :sharedLogic:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…