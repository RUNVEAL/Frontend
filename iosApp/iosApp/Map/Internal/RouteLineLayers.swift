import MapLibre

/// 가본 길 선 레이어 — 소스 1개 + 코어 LineLayer([glowColor]를 주면 글로우 2개 더).
/// [sourceId]를 호출하는 쪽이 넘겨서, 홈 지도의 누적 "가본 길"과 러닝 중 실시간 선(이미 가본 구간/
/// 새 구간)이 서로 다른 소스·레이어를 쓰게 한다 — 레이어 ID는 소스 ID에서 그대로 파생시킨다.
///
/// 블러·두께·줌 보간 값은 토큰에 없는 지도 SDK 전용 튜닝이라 Android와 같은 숫자를 여기 상수로 둔다.
///
/// [minimumZoom]보다 축소하면 숨긴다. **홈의 누적 가본 길은 그 하한을 쓰지 않는다**(v2) — 길이
/// 보이냐 동네가 보이냐를 이제 홈 토글이 정해서, 줌으로도 가리면 "토글로만"이 깨진다. 러닝 중
/// 실시간 선은 기본값을 그대로 둔다.
enum VisitedRoadLayer {
    private static let glowWidth: Double = 16
    private static let glowOpacity: Double = 0.45
    private static let glowBlur: Double = 14

    private static let coreWidth: Double = 3
    private static let coreOpacity: Double = 1

    private static let zoomSmall: Double = 12
    private static let zoomBase: Double = 16
    private static let smallScale: Double = 0.4

    static func apply(
        style: MLNStyle,
        sourceId: String,
        geoJson: String?,
        coreColor: UIColor,
        glowColor: UIColor? = nil,
        minimumZoom: Double? = RunvealMapConfig.zoomInThreshold
    ) {
        MapSourceSupport.upsertShapeSource(style: style, sourceId: sourceId, geoJson: geoJson) { source in
            if let glowColor {
                addLineLayer(
                    style: style,
                    source: source,
                    layerId: "\(sourceId)-glow-outer",
                    color: glowColor,
                    width: glowWidth,
                    opacity: glowOpacity,
                    blur: glowBlur,
                    minimumZoom: minimumZoom
                )
                addLineLayer(
                    style: style,
                    source: source,
                    layerId: "\(sourceId)-glow-mid",
                    color: glowColor,
                    width: glowWidth / 2,
                    opacity: glowOpacity,
                    blur: glowBlur / 2,
                    minimumZoom: minimumZoom
                )
            }
            addLineLayer(
                style: style,
                source: source,
                layerId: "\(sourceId)-line",
                color: coreColor,
                width: coreWidth,
                opacity: coreOpacity,
                blur: 0,
                minimumZoom: minimumZoom
            )
        }
    }

    private static func addLineLayer(
        style: MLNStyle,
        source: MLNShapeSource,
        layerId: String,
        color: UIColor,
        width: Double,
        opacity: Double,
        blur: Double,
        minimumZoom: Double?
    ) {
        guard style.layer(withIdentifier: layerId) == nil else { return }
        let layer = MLNLineStyleLayer(identifier: layerId, source: source)
        layer.lineCap = NSExpression(forConstantValue: "round")
        layer.lineJoin = NSExpression(forConstantValue: "round")
        layer.lineColor = NSExpression(forConstantValue: color)
        layer.lineWidth = MapExpressions.zoomInterpolated(
            base: width,
            zoomSmall: zoomSmall,
            zoomBase: zoomBase,
            smallScale: smallScale
        )
        layer.lineOpacity = NSExpression(forConstantValue: opacity)
        layer.lineBlur = MapExpressions.zoomInterpolated(
            base: blur,
            zoomSmall: zoomSmall,
            zoomBase: zoomBase,
            smallScale: smallScale
        )
        if let minimumZoom { layer.minimumZoomLevel = Float(minimumZoom) }
        style.addLayer(layer)
    }
}

/// 추천 코스 레이어 — 소스 1개 + LineLayer 3개(선택 안 된 코스, 선택된 코스 글로우, 선택된 코스 코어).
/// 피처의 `selected` 속성으로 필터링해서 선택된 코스만 강조한다.
///
/// [sourceId]를 호출하는 쪽이 넘긴다 — 코스 추천 결과(여러 코스)와 코스 상세(코스 하나)가 서로 다른
/// 소스를 쓰게 해서, 두 화면 전환 시점에 한쪽의 레이어 정리가 다른 쪽이 막 그린 선을 지워버리는
/// 경합이 생기지 않게 한다.
enum RecommendedRouteLayer {
    private static let altWidth: Double = 3
    private static let altOpacity: Double = 0.7

    private static let glowWidth: Double = 12
    private static let glowOpacity: Double = 0.5
    private static let glowBlur: Double = 10

    private static let coreWidth: Double = 4

    static func apply(style: MLNStyle, sourceId: String, geoJson: String?, colors: RunvealMapColors) {
        MapSourceSupport.upsertShapeSource(style: style, sourceId: sourceId, geoJson: geoJson) { source in
            addLineLayer(
                style: style,
                source: source,
                layerId: "\(sourceId)-alt",
                color: colors.recommendedRouteAlt,
                width: altWidth,
                opacity: altOpacity,
                blur: 0,
                selected: false
            )
            addLineLayer(
                style: style,
                source: source,
                layerId: "\(sourceId)-selected-glow",
                color: colors.recommendedRouteSelectedGlow,
                width: glowWidth,
                opacity: glowOpacity,
                blur: glowBlur,
                selected: true
            )
            addLineLayer(
                style: style,
                source: source,
                layerId: "\(sourceId)-selected",
                color: colors.recommendedRouteSelected,
                width: coreWidth,
                opacity: 1,
                blur: 0,
                selected: true
            )
        }
    }

    private static func addLineLayer(
        style: MLNStyle,
        source: MLNShapeSource,
        layerId: String,
        color: UIColor,
        width: Double,
        opacity: Double,
        blur: Double,
        selected: Bool
    ) {
        guard style.layer(withIdentifier: layerId) == nil else { return }
        let layer = MLNLineStyleLayer(identifier: layerId, source: source)
        layer.lineCap = NSExpression(forConstantValue: "round")
        layer.lineJoin = NSExpression(forConstantValue: "round")
        layer.lineColor = NSExpression(forConstantValue: color)
        layer.lineWidth = NSExpression(forConstantValue: width)
        layer.lineOpacity = NSExpression(forConstantValue: opacity)
        layer.lineBlur = NSExpression(forConstantValue: blur)
        layer.predicate = NSPredicate(format: "selected == %@", NSNumber(value: selected))
        style.addLayer(layer)
    }
}

/// 러닝 중("이 코스로 러닝 시작") 지도에 그리는 추천 코스 안내선 — 흰색 점선 하나뿐이라
/// [RecommendedRouteLayer]처럼 glow/selected로 여러 겹 두지 않는다.
enum CourseGuideLayer {
    private static let lineWidth: Double = 3

    /// `lineWidth`의 배수 단위 — [대시, 간격]. 일반 점선보다 살짝 긴 대시, 간격은 촘촘하게.
    private static let dashPattern: [Double] = [1.4, 0.5]

    static func apply(style: MLNStyle, sourceId: String, geoJson: String?, colors: RunvealMapColors) {
        MapSourceSupport.upsertShapeSource(style: style, sourceId: sourceId, geoJson: geoJson) { source in
            let layerId = "\(sourceId)-line"
            guard style.layer(withIdentifier: layerId) == nil else { return }
            let layer = MLNLineStyleLayer(identifier: layerId, source: source)
            layer.lineCap = NSExpression(forConstantValue: "butt")
            layer.lineJoin = NSExpression(forConstantValue: "round")
            layer.lineColor = NSExpression(forConstantValue: colors.courseGuide)
            layer.lineWidth = NSExpression(forConstantValue: lineWidth)
            layer.lineDashPattern = NSExpression(forConstantValue: dashPattern)

            // "오늘 달린 길" 바로 위에 그린다 — 효과 실행 순서에 기대지 않고 명시적으로 고정한다.
            if let visitedLine = style.layer(withIdentifier: RunvealMapConfig.layerVisitedRoadLine) {
                style.insertLayer(layer, above: visitedLine)
            } else {
                style.addLayer(layer)
            }
        }
    }
}
