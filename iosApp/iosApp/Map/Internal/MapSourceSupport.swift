import MapLibre
import SharedLogic

/// GeoJSON 문자열 ↔ MapLibre 소스 다루기. Android는 `GeoJsonSource.setGeoJson(String)` 하나로
/// 끝나지만, iOS는 `MLNShape`로 한 번 파싱해서 넣어야 한다.
enum MapSourceSupport {

    /// 소스에 **마지막으로 올린** GeoJSON — 같은 내용을 다시 파싱해 올리지 않으려고 기억해 둔다.
    ///
    /// 지도 상태가 한 번 바뀔 때마다 모든 소스를 다시 올리던 게 문제였다. 전국 동 경계처럼 큰
    /// GeoJSON은 파싱만으로도 메인 스레드를 오래 잡아서, 지도가 멈췄다가 한 번에 따라잡는 것처럼
    /// 보인다. Android는 레이어마다 `LaunchedEffect(style, 그_geoJson, …)`으로 묶여 바뀔 때만
    /// 올라가는데, 여기서 같은 결과를 만든다.
    private static var appliedGeoJson: [String: String] = [:]

    /// 위 캐시가 어느 스타일 기준인지 — 스타일이 다시 로드되면 소스도 사라지므로 통째로 버린다.
    ///
    /// 전부 지도 코드(메인 스레드)에서만 건드린다 — 그래서 따로 잠그지 않는다.
    private static weak var cachedStyle: MLNStyle?

    static var emptyFeatureCollectionJson: String {
        IosRouteGeoJson.shared.empty
    }

    static func shape(from geoJson: String) -> MLNShape? {
        guard let data = geoJson.data(using: .utf8) else { return nil }
        return try? MLNShape(data: data, encoding: String.Encoding.utf8.rawValue)
    }

    /// [sourceId] 소스가 없으면 만들고, 있으면 도형만 갈아 끼운다.
    /// 소스를 **새로 만든 경우에만** [onCreate]를 불러 레이어를 한 번 붙인다 — Android의
    /// `if (source == null) { addSource(); ensureLayers() }` 패턴과 같다.
    static func upsertShapeSource(
        style: MLNStyle,
        sourceId: String,
        geoJson: String?,
        onCreate: (MLNShapeSource) -> Void
    ) {
        let json = geoJson ?? emptyFeatureCollectionJson

        if cachedStyle !== style {
            cachedStyle = style
            appliedGeoJson.removeAll()
        }

        if let existing = style.source(withIdentifier: sourceId) as? MLNShapeSource {
            // 내용이 그대로면 아무것도 하지 않는다 — 파싱도 업로드도 건너뛴다.
            guard appliedGeoJson[sourceId] != json else { return }
            appliedGeoJson[sourceId] = json
            existing.shape = shape(from: json)
            return
        }
        appliedGeoJson[sourceId] = json
        let source = MLNShapeSource(identifier: sourceId, shape: shape(from: json), options: nil)
        style.addSource(source)
        onCreate(source)
    }
}
