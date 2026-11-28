import MapLibre
import SharedLogic

/// 동마다 [DongGlowProvider]가 구해준 빛 이미지를 `MLNImageSource`+`MLNRasterStyleLayer`로
/// `dong-fill`과 `dong-line` 사이에 올린다 — Android `DongGlowImageLayer`에 해당한다.
///
/// 계산·캐시는 전부 [DongGlowProvider] 몫이고, 여기서는 화면에 보이는 동을 추려서 요청하고 결과를
/// 지도에 얹기만 한다.
@MainActor
final class DongGlowImageLayer {
    private let provider: DongGlowProvider
    private var activeAdmCd2: Set<String> = []

    /// 한 번에 이만큼 넘게 걸리면 로그로 알린다(Android와 같은 기준).
    private static let visibleCountWarningThreshold = 60

    init(provider: DongGlowProvider) {
        self.provider = provider
    }

    /// 단계 ≥ 1이고 화면에 보이는 동만 추려 빛 이미지를 요청한다 — **추리는 일은 Kotlin
    /// [DongOverlayQuery]가 한다.** 전국 폴리곤을 Swift로 넘겨 여기서 훑으면 좌표 하나하나가
    /// ObjC 브리지를 타서 메인 스레드가 길게 잡힌다. 화면 중심에 가까운 순으로 돌려받아 그 순서대로
    /// 부르고, 더 이상 보이지 않는 동은 레이어를 지우고 대기 중인 생성 작업도 취소한다.
    func refresh(
        style: MLNStyle,
        mapView: MLNMapView,
        query: DongOverlayQuery?,
        boundarySourceKey: String
    ) async {
        // 러닝 기록 중·코스 추천 중처럼 동 오버레이를 숨길 때는 nil이거나 빈 창구가 들어온다. 그냥
        // 돌아가면 테두리·라벨만 사라지고 이미 올라간 빛 이미지는 지도에 그대로 남는다 — 같이 내린다.
        guard let query, !query.isEmpty else {
            removeAllDongs(style: style)
            return
        }

        let visibleBounds = mapView.visibleCoordinateBounds
        let cameraCenter = mapView.centerCoordinate

        let candidates = query.glowCandidates(
            minLat: visibleBounds.sw.latitude,
            minLon: visibleBounds.sw.longitude,
            maxLat: visibleBounds.ne.latitude,
            maxLon: visibleBounds.ne.longitude,
            centerLat: cameraCenter.latitude,
            centerLon: cameraCenter.longitude
        )

        if candidates.count > Self.visibleCountWarningThreshold {
            print("[DongGlowImageLayer] 한 번에 \(candidates.count)개 동 빛 이미지가 화면에 걸림 "
                + "(주의 기준 \(Self.visibleCountWarningThreshold))")
        }

        provider.cancelExcept(
            stillWantedKeys: Set(candidates.map {
                // Kotlin `Int`는 Swift에 `Int32`로 넘어온다.
                DongGlowProvider.jobKey(admCd2: $0.admCd2, level: Int($0.level))
            })
        )

        let target = Set(candidates.map(\.admCd2))
        for admCd2 in activeAdmCd2.subtracting(target) {
            removeDong(style: style, admCd2: admCd2)
            activeAdmCd2.remove(admCd2)
        }

        for candidate in candidates {
            // 이미지를 실제로 그릴 동만 폴리곤을 꺼낸다 — 전국이 아니라 화면에 걸린 수십 개뿐이다.
            let parts = query.polygonParts(admCd2: candidate.admCd2)
            guard !parts.isEmpty else { continue }
            guard let asset = await provider.asset(
                admCd2: candidate.admCd2,
                level: Int(candidate.level),
                parts: parts,
                boundarySourceKey: boundarySourceKey
            ) else { continue }
            addOrUpdate(style: style, admCd2: candidate.admCd2, asset: asset)
            activeAdmCd2.insert(candidate.admCd2)
        }
    }

    private func addOrUpdate(style: MLNStyle, admCd2: String, asset: DongGlowAsset) {
        guard asset.corners.count == 4 else { return }
        let quad = MLNCoordinateQuad(
            topLeft: asset.corners[0].coordinate,
            bottomLeft: asset.corners[3].coordinate,
            bottomRight: asset.corners[2].coordinate,
            topRight: asset.corners[1].coordinate
        )

        let sourceId = Self.sourceId(admCd2)
        if let existing = style.source(withIdentifier: sourceId) as? MLNImageSource {
            existing.image = asset.image
            existing.coordinates = quad
        } else {
            let source = MLNImageSource(identifier: sourceId, coordinateQuad: quad, image: asset.image)
            style.addSource(source)
        }

        let layerId = Self.layerId(admCd2)
        guard style.layer(withIdentifier: layerId) == nil else { return }
        guard let source = style.source(withIdentifier: sourceId) else { return }
        let layer = MLNRasterStyleLayer(identifier: layerId, source: source)
        layer.rasterResamplingMode = NSExpression(forConstantValue: "linear")
        layer.rasterFadeDuration = NSExpression(forConstantValue: 0)
        layer.minimumZoomLevel = Float(RunvealMapConfig.sigunguSwitchZoom)
        // 줌 상한 없음(v2) — 동 경계와 같이 켜지고 꺼져야 해서 홈 토글만이 가린다.
        // 레이어 순서: dong-fill 위, dong-line 아래.
        if let dongLine = style.layer(withIdentifier: RunvealMapConfig.layerDongLine) {
            style.insertLayer(layer, below: dongLine)
        } else {
            style.addLayer(layer)
        }
    }

    /// 올라가 있는 동 빛 이미지를 전부 내리고 대기 중인 생성 작업도 취소한다.
    private func removeAllDongs(style: MLNStyle) {
        guard !activeAdmCd2.isEmpty else { return }
        provider.cancelExcept(stillWantedKeys: [])
        for admCd2 in activeAdmCd2 {
            removeDong(style: style, admCd2: admCd2)
        }
        activeAdmCd2.removeAll()
    }

    private func removeDong(style: MLNStyle, admCd2: String) {
        if let layer = style.layer(withIdentifier: Self.layerId(admCd2)) {
            style.removeLayer(layer)
        }
        if let source = style.source(withIdentifier: Self.sourceId(admCd2)) {
            style.removeSource(source)
        }
    }

    private static func sourceId(_ admCd2: String) -> String { "dong-glow-img-src-\(admCd2)" }
    private static func layerId(_ admCd2: String) -> String { "dong-glow-img-\(admCd2)" }
}
