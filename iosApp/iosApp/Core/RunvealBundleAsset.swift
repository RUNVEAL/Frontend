import Foundation

/// 앱 번들에 들어 있는 공용 원본 에셋(전국 경계 GeoJSON, 로딩 모션 JSON 등)을 읽는다.
///
/// 원본은 아직 Android 모듈 안에 있고(`androidApp/core/map/src/main/assets/dong`,
/// `androidApp/core/designsystem/src/main/res/raw`), 빌드할 때 Gradle이
/// iosApp/iosApp/Resources/Generated로 복사해 둔다(gradle/runveal-bundle-assets-ios.gradle.kts).
///
/// Xcode가 번들 루트에 평평하게 복사하므로 Android처럼 디렉터리를 포함한 경로가 아니라 **파일
/// 이름만으로** 찾는다 — Android `assets/dong/korea_dong.geojson` ↔ iOS `korea_dong.geojson`.
enum RunvealBundleAsset {
    enum Failure: Error, LocalizedError {
        case notFound(name: String, extension: String)

        var errorDescription: String? {
            switch self {
            case .notFound(let name, let ext):
                return "번들에 \(name).\(ext)이 없다 — Gradle generateRunvealIosResources가 돌았는지 확인한다."
            }
        }
    }

    /// 경계 GeoJSON 전국 시군구 — Android `dong/korea_sigungu.geojson`.
    static let koreaSigunguGeoJson = Name("korea_sigungu", "geojson")
    /// 경계 GeoJSON 전국 시도 — Android `dong/korea_sido.geojson`.
    static let koreaSidoGeoJson = Name("korea_sido", "geojson")
    /// 시도 → 시군구 → 행정동 계층 — Android `dong/korea_region_hierarchy.json`.
    static let koreaRegionHierarchy = Name("korea_region_hierarchy", "json")
    /// 공용 로딩 모션 — Android `res/raw/runveal_loading_common.json`.
    static let loadingMotion = Name("runveal_loading_common", "json")

    struct Name {
        let resource: String
        let fileExtension: String

        init(_ resource: String, _ fileExtension: String) {
            self.resource = resource
            self.fileExtension = fileExtension
        }
    }

    static func url(_ name: Name) throws -> URL {
        guard let url = Bundle.main.url(forResource: name.resource, withExtension: name.fileExtension) else {
            throw Failure.notFound(name: name.resource, extension: name.fileExtension)
        }
        return url
    }

    static func text(_ name: Name) throws -> String {
        try String(contentsOf: url(name), encoding: .utf8)
    }

    static func data(_ name: Name) throws -> Data {
        try Data(contentsOf: url(name))
    }

    /// 없으면 `nil` — Android가 번들 에셋이 없을 때 조용히 빈 값으로 떨어지는 자리에 쓴다.
    static func textIfPresent(_ name: Name) -> String? {
        try? text(name)
    }
}
