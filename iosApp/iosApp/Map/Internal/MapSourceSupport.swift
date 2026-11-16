import MapLibre
import SharedLogic

/// GeoJSON 문자열 ↔ MapLibre 소스 다루기. Android는 `GeoJsonSource.setGeoJson(String)` 하나로
/// 끝나지만, iOS는 `MLNShape`로 한 번 파싱해서 넣어야 한다.
enum MapSourceSupport {

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
        let newShape = shape(from: json)

        if let existing = style.source(withIdentifier: sourceId) as? MLNShapeSource {
            existing.shape = newShape
            return
        }
        let source = MLNShapeSource(identifier: sourceId, shape: newShape, options: nil)
        style.addSource(source)
        onCreate(source)
    }
}
