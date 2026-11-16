/*
 * Android 모듈 안에 있는 공용 원본 에셋을 iOS 번들로 복사
 *
 * 두 플랫폼이 똑같이 쓰는 원본이지만 아직 Android 모듈 안(`src/main/assets`, `src/main/res/raw`)에
 * 들어 있는 파일들이다. 저장소에 같은 파일을 두 번 넣지 않도록, 빌드할 때
 * iosApp/iosApp/Resources/Generated로 복사해 번들에 넣는다(생성물이라 .gitignore 대상).
 *
 *  - 전국 동/시군구/시도 경계 GeoJSON(약 6.6MB) — 홈 지도의 동 오버레이 원본
 *  - 공용 로딩 모션 Lottie JSON — RunvealLoading
 *
 * Xcode는 File System Synchronized Group 안의 리소스를 번들 루트에 평평하게 복사하므로, iOS에서는
 * Android처럼 "dong/korea_dong.geojson"이 아니라 파일 이름만으로 찾는다
 * (iosApp/iosApp/Core/RunvealBundleAsset.swift).
 *
 * TODO: 공용 원본이 Android 모듈 안에 있는 게 플랫폼 중립적이지 않다. design/처럼 저장소 최상위
 *  (예: assets/)로 옮기면 이 복사 태스크의 `from`과 Android 쪽 sourceSets 선언만 바꾸면 된다.
 */

val generateRunvealMapAssetsIos = tasks.register<Copy>("generateRunvealMapAssetsIos") {
    group = "runveal"
    description = "동/시군구/시도 경계 에셋 -> iOS 번들 리소스로 복사"
    from(rootProject.layout.projectDirectory.dir("androidApp/core/map/src/main/assets/dong")) {
        include("*.geojson", "*.json")
    }
    into(rootProject.layout.projectDirectory.dir("iosApp/iosApp/Resources/Generated"))
}

val generateRunvealMotionAssetsIos = tasks.register<Copy>("generateRunvealMotionAssetsIos") {
    group = "runveal"
    description = "공용 로딩 모션(Lottie JSON) -> iOS 번들 리소스로 복사"
    from(rootProject.layout.projectDirectory.dir("androidApp/core/designsystem/src/main/res/raw")) {
        include("runveal_loading_common.json")
    }
    into(rootProject.layout.projectDirectory.dir("iosApp/iosApp/Resources/Generated"))
}

/**
 * Xcode "Generate Runveal iOS Resources" Run Script 빌드 단계가 호출하는 묶음 태스크 —
 * 토큰·아이콘·이미지·번들 에셋을 한 번에 만든다. 생성기를 추가하면 여기에만 끼워 넣으면 되고
 * pbxproj는 더 안 건드려도 된다.
 */
val generateRunvealIosResources = tasks.register("generateRunvealIosResources") {
    group = "runveal"
    description = "iOS 번들에 필요한 생성물 전부(디자인 토큰, 아이콘, 이미지, 경계·모션 에셋)"
    dependsOn(
        "generateRunvealTokensSwift",
        "generateRunvealIconsSwift",
        "generateRunvealImagesSwift",
        generateRunvealMapAssetsIos,
        generateRunvealMotionAssetsIos,
    )
}
