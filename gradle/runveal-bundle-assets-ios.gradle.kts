/*
 * Android 모듈 안에 있는 공용 원본 에셋을 iOS 번들로 복사
 *
 * 두 플랫폼이 똑같이 쓰는 원본이지만 아직 Android 모듈 안(`src/main/assets`, `src/main/res/raw`)에
 * 들어 있는 파일들이다. 저장소에 같은 파일을 두 번 넣지 않도록, 빌드할 때
 * iosApp/iosApp/Resources/Generated로 복사해 번들에 넣는다(생성물이라 .gitignore 대상).
 *
 *  - 시군구/시도 경계 GeoJSON과 지역 계층 — 홈 지도의 상위 레벨 오버레이, 동 이름 조회
 *  - 공용 로딩 모션 Lottie JSON — RunvealLoading
 *
 * **전국 동 경계(`korea_dong.geojson`)는 여기 없다.** 2026-11부터 번들에서 빠졌고 서버
 * `index.geojson`에서 받는다 — 번들 스냅샷은 서버와 행정동 코드 체계가 약 13% 달라서 완주율이
 * 조용히 틀어졌다(`androidApp/core/map/src/main/assets/dong/SOURCE.md`).
 *
 * Xcode는 File System Synchronized Group 안의 리소스를 번들 루트에 평평하게 복사하므로, iOS에서는
 * Android처럼 "dong/korea_sigungu.geojson"이 아니라 파일 이름만으로 찾는다
 * (iosApp/iosApp/Core/RunvealBundleAsset.swift).
 *
 * TODO: 공용 원본이 Android 모듈 안에 있는 게 플랫폼 중립적이지 않다. design/처럼 저장소 최상위
 *  (예: assets/)로 옮기면 이 복사 태스크의 `from`과 Android 쪽 sourceSets 선언만 바꾸면 된다.
 */

/**
 * `Copy`가 아니라 `Sync`다 — 이 태스크가 출력 디렉터리를 **통째로 소유**해서, 원본에서 지워진 파일이
 * iOS 쪽에 남지 않는다. `Copy`였을 때 번들에서 빠진 `korea_dong.geojson`(4.3MB)이 생성 폴더에 그대로
 * 남아 IPA에 실리고, Swift가 그 옛 스냅샷을 계속 읽었다.
 *
 * 그래서 경계 에셋과 모션 에셋을 **한 태스크로 합쳐 뒀다** — 같은 디렉터리에 쓰는 `Sync`가 둘이면
 * 서로의 출력을 지운다.
 */
val generateRunvealBundleAssetsIos = tasks.register<Sync>("generateRunvealBundleAssetsIos") {
    group = "runveal"
    description = "경계·계층 에셋과 로딩 모션 -> iOS 번들 리소스로 동기화"

    from(rootProject.layout.projectDirectory.dir("androidApp/core/map/src/main/assets/dong")) {
        // SOURCE.md 같은 문서는 번들에 넣지 않는다.
        include("*.geojson", "*.json")
    }
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
        generateRunvealBundleAssetsIos,
    )
}
