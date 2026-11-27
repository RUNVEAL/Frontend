plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.firebaseCrashlytics) apply false
}

// iOS(iosApp) 빌드에 필요한 생성물. Kotlin/Android 쪽 같은 생성기는 각 모듈 build.gradle.kts에서 적용한다.
// design/runveal-tokens.json → Swift 디자인 토큰
apply(from = "gradle/runveal-tokens-swift.gradle.kts")
// design/icons/*.svg → RunvealIcons.swift
apply(from = "gradle/runveal-icons-swift.gradle.kts")
// design/images/* → iOS 번들 리소스 + RunvealImages.swift
apply(from = "gradle/runveal-images-swift.gradle.kts")
// Android 모듈에 있는 공용 원본 에셋(경계 GeoJSON, 로딩 모션) → iOS 번들 리소스
// (+ 위 생성물까지 묶은 generateRunvealIosResources)
apply(from = "gradle/runveal-bundle-assets-ios.gradle.kts")