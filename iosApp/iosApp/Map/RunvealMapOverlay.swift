import Foundation
import SharedLogic

/// 동 탐험률 오버레이 — 채우기/테두리/라벨 레이어 각각에 들어갈 GeoJSON `FeatureCollection` 문자열.
/// Android `core:map`의 `DongOverlayGeoJson`.
struct DongOverlayGeoJson {
    let fillGeoJson: String
    let lineGeoJson: String
    let labelGeoJson: String
}

/// 시군구·시도 경계 오버레이 — 테두리/라벨만(면은 없다).
/// Android `SigunguOverlayGeoJson`/`SidoOverlayGeoJson`(모양이 같아 하나로 합쳤다).
struct RegionOverlayGeoJson {
    let lineGeoJson: String
    let labelGeoJson: String
}

/// 홈 지도에 한 번에 얹는 동/시군구/시도 오버레이 묶음 — Android `DongOverlayBundle`.
struct RunvealDongOverlayBundle {
    let dong: DongOverlayGeoJson
    let sigungu: RegionOverlayGeoJson
    let sido: RegionOverlayGeoJson
    /// 빛 이미지 생성에 쓰는, 구멍을 보존한 전국 동 폴리곤 — 테두리 선과 같은 정점을 쓴다.
    let glowPolygonParts: [String: [DongPolygonPart]]
    /// 행정동 코드 → 단계(0~6). 빛 이미지를 올릴 동을 고르는 데 쓴다.
    let dongLevels: [String: Int]
    /// `"asset"` 또는 `"server:<지도 버전>"`. 빛 이미지 캐시 키에 들어가, 서버가 경계를 고쳐
    /// 배포하면 옛 이미지를 쓰지 않는다.
    let boundarySourceKey: String
    /// 이 묶음을 만들 때 반영돼 있던 마지막 러닝 기록의 ID(없으면 nil) — 홈이 "지금 내 기록 상태와
    /// 같은 오버레이인지"를 판단해서 괜히 다시 만들지 않는 데 쓴다.
    let builtForTrackId: String?

    init(bundle: DongOverlayGeoJsonBundle, builtForTrackId: String?) {
        dong = DongOverlayGeoJson(
            fillGeoJson: bundle.dongFillGeoJson,
            lineGeoJson: bundle.dongLineGeoJson,
            labelGeoJson: bundle.dongLabelGeoJson
        )
        sigungu = RegionOverlayGeoJson(
            lineGeoJson: bundle.sigunguLineGeoJson,
            labelGeoJson: bundle.sigunguLabelGeoJson
        )
        sido = RegionOverlayGeoJson(
            lineGeoJson: bundle.sidoLineGeoJson,
            labelGeoJson: bundle.sidoLabelGeoJson
        )
        glowPolygonParts = bundle.dongPolygonPartsByAdmCd2 as? [String: [DongPolygonPart]] ?? [:]
        dongLevels = (bundle.dongLevelByAdmCd2 as? [String: NSNumber] ?? [:]).mapValues { $0.intValue }
        boundarySourceKey = bundle.boundarySourceKey
        self.builtForTrackId = builtForTrackId
    }
}

/// 만들어 둔 오버레이를 스플래시 → 홈으로 건네주는 싱글톤 — Android `DongOverlayHolder`.
///
/// 전국 경계 GeoJSON을 파싱하고 동별 탐험률을 집계하는 데 몇 초가 걸려서, 홈에 들어온 뒤에 시작하면
/// 지도가 한참 비어 있다가 뒤늦게 동이 떠오른다 — 스플래시가 미리 만들어 여기 담아 두면 홈은 첫
/// 프레임부터 그릴 수 있다.
@MainActor
final class RunvealDongOverlayHolder: ObservableObject {
    static let shared = RunvealDongOverlayHolder()

    @Published private(set) var bundle: RunvealDongOverlayBundle?

    private init() {}

    func publish(_ bundle: RunvealDongOverlayBundle) {
        self.bundle = bundle
    }
}

/// 내 기록 기준 동 오버레이 묶음을 만든다 — 스플래시의 사전 생성, 러닝 종료 후 홈의 갱신 둘 다 이 함수.
/// 실제 계산은 전부 sharedLogic(`IosDongOverlayLoader`)이 한다.
enum RunvealDongOverlayLoader {
    static func load(builtForTrackId: String?) async throws -> RunvealDongOverlayBundle {
        // 동 경계와 완주율 분모는 번들이 아니라 서버 index.geojson/index.json에서 온다
        // (IosServerDongData) — 번들 스냅샷은 서버와 행정동 코드 체계가 달라 완주율이 틀어진다.
        // 시군구·시도 경계와 계층은 서버가 주지 않는 자료라 번들에 남아 있다.
        let assets = DongBundleAssets(
            sigunguGeoJson: try RunvealBundleAsset.text(RunvealBundleAsset.koreaSigunguGeoJson),
            sidoGeoJson: try RunvealBundleAsset.text(RunvealBundleAsset.koreaSidoGeoJson),
            regionHierarchyJson: try RunvealBundleAsset.text(RunvealBundleAsset.koreaRegionHierarchy)
        )
        let bundle = try await IosDongOverlayLoader.shared.load(assets: assets)
        return RunvealDongOverlayBundle(bundle: bundle, builtForTrackId: builtForTrackId)
    }
}
