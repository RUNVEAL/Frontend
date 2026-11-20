import MapLibre

/// 지도 라벨 글꼴 — 기본 스타일(OpenFreeMap dark)의 글리프 세트에 있는 이름을 그대로 쓴다.
private let labelFontNames = ["Noto Sans Regular"]

/// 동 채우기/테두리/라벨.
///
/// 면은 탐험률 단계와 무관하게 0단계(투명) 고정이다 — 단계별 밝기는 [DongGlowImageLayer]가 올리는
/// 빛 이미지가 전담하므로, 그 아래 면까지 단계별로 다르면 이미지 로딩 전후로 같은 동이 두 번 다른
/// 색으로 보인다. 테두리는 반대로 단계별 색을 그대로 써서, 빛 이미지가 뜨기 전에도 테두리만으로
/// 단계를 알 수 있게 한다.
///
/// `SIGUNGU_SWITCH_ZOOM`~`ZOOM_IN_THRESHOLD` 사이에서만 보인다.
enum DongOverlayLayer {
    private static let lineWidth: Double = 1.2
    private static let labelSize: Double = 11

    static func apply(style: MLNStyle, overlay: DongOverlayGeoJson?, colors: RunvealMapColors) {
        MapSourceSupport.upsertShapeSource(
            style: style,
            sourceId: RunvealMapConfig.sourceDongFill,
            geoJson: overlay?.fillGeoJson
        ) { source in
            let layer = MLNFillStyleLayer(identifier: RunvealMapConfig.layerDongFill, source: source)
            layer.fillColor = NSExpression(forConstantValue: UIColor(RunvealDong.style(level: 0).fill))
            layer.minimumZoomLevel = Float(RunvealMapConfig.sigunguSwitchZoom)
            layer.maximumZoomLevel = Float(RunvealMapConfig.zoomInThreshold)
            style.addLayer(layer)
        }

        MapSourceSupport.upsertShapeSource(
            style: style,
            sourceId: RunvealMapConfig.sourceDongLine,
            geoJson: overlay?.lineGeoJson
        ) { source in
            let layer = MLNLineStyleLayer(identifier: RunvealMapConfig.layerDongLine, source: source)
            layer.lineCap = NSExpression(forConstantValue: "round")
            layer.lineJoin = NSExpression(forConstantValue: "round")
            layer.lineColor = MapExpressions.levelColorStep(maxLevel: RunvealDong.maxLevel) { level in
                UIColor(RunvealDong.style(level: level).stroke)
            }
            layer.lineWidth = NSExpression(forConstantValue: lineWidth)
            layer.minimumZoomLevel = Float(RunvealMapConfig.sigunguSwitchZoom)
            layer.maximumZoomLevel = Float(RunvealMapConfig.zoomInThreshold)
            style.addLayer(layer)
        }

        MapSourceSupport.upsertShapeSource(
            style: style,
            sourceId: RunvealMapConfig.sourceDongLabel,
            geoJson: overlay?.labelGeoJson
        ) { source in
            let layer = MLNSymbolStyleLayer(identifier: RunvealMapConfig.layerDongLabel, source: source)
            layer.text = MapExpressions.dongLabelText
            layer.textFontNames = NSExpression(forConstantValue: labelFontNames)
            layer.textFontSize = NSExpression(forConstantValue: labelSize)
            layer.textColor = NSExpression(forConstantValue: colors.dongLabelPrimary)
            // 겹쳐서 하나만 남을 때 탐험률 높은 동을 살린다.
            layer.symbolSortKey = MapExpressions.labelSortKeyByPercent
            layer.minimumZoomLevel = Float(RunvealMapConfig.sigunguSwitchZoom)
            layer.maximumZoomLevel = Float(RunvealMapConfig.zoomInThreshold)
            style.addLayer(layer)
        }
    }
}

/// 시군구 테두리/라벨 — `SIDO_SWITCH_ZOOM`~`SIGUNGU_SWITCH_ZOOM` 사이에서만 보인다.
/// fill은 안 만든다(동 레벨과 같은 이유로 면은 빛 이미지가 전담). 테두리 색은 [RunvealDong] 단계별
/// 색 토큰을 그대로 재사용한다 — 시군구 전용 색 토큰은 따로 없고, 소속 동들의 탐험률을 집계한 값이라
/// 같은 단계 체계를 쓴다.
enum SigunguOverlayLayer {
    private static let lineWidth: Double = 1.5
    private static let labelSize: Double = 12

    static func apply(style: MLNStyle, overlay: RegionOverlayGeoJson?, colors: RunvealMapColors) {
        RegionOverlayLayerSupport.apply(
            style: style,
            overlay: overlay,
            colors: colors,
            lineSourceId: RunvealMapConfig.sourceSigunguLine,
            labelSourceId: RunvealMapConfig.sourceSigunguLabel,
            lineLayerId: RunvealMapConfig.layerSigunguLine,
            labelLayerId: RunvealMapConfig.layerSigunguLabel,
            lineWidth: lineWidth,
            labelSize: labelSize,
            // 줌아웃할수록 시군구가 촘촘해져서 다 띄우면 이름이 서로 겹쳐 읽을 수 없다 —
            // 겹치면 숨기고, 누가 남을지는 탐험률로 정한다(동 라벨과 같은 규칙).
            allowsLabelOverlap: false,
            minimumZoomLevel: RunvealMapConfig.sidoSwitchZoom,
            maximumZoomLevel: RunvealMapConfig.sigunguSwitchZoom
        )
    }
}

/// 시도 테두리/라벨 — `SIDO_SWITCH_ZOOM`보다 축소했을 때만 보인다.
enum SidoOverlayLayer {
    private static let lineWidth: Double = 2
    private static let labelSize: Double = 13

    static func apply(style: MLNStyle, overlay: RegionOverlayGeoJson?, colors: RunvealMapColors) {
        RegionOverlayLayerSupport.apply(
            style: style,
            overlay: overlay,
            colors: colors,
            lineSourceId: RunvealMapConfig.sourceSidoLine,
            labelSourceId: RunvealMapConfig.sourceSidoLabel,
            lineLayerId: RunvealMapConfig.layerSidoLine,
            labelLayerId: RunvealMapConfig.layerSidoLabel,
            lineWidth: lineWidth,
            labelSize: labelSize,
            // 17개뿐이라 겹칠 일이 거의 없다 — 항상 보이게 둔다.
            allowsLabelOverlap: true,
            minimumZoomLevel: nil,
            maximumZoomLevel: RunvealMapConfig.sidoSwitchZoom
        )
    }
}

/// 시군구·시도는 입력(소스/레이어 ID, 두께, 줌 범위)만 다르고 만드는 모양이 완전히 같다.
private enum RegionOverlayLayerSupport {
    static func apply(
        style: MLNStyle,
        overlay: RegionOverlayGeoJson?,
        colors: RunvealMapColors,
        lineSourceId: String,
        labelSourceId: String,
        lineLayerId: String,
        labelLayerId: String,
        lineWidth: Double,
        labelSize: Double,
        allowsLabelOverlap: Bool,
        minimumZoomLevel: Double?,
        maximumZoomLevel: Double
    ) {
        MapSourceSupport.upsertShapeSource(style: style, sourceId: lineSourceId, geoJson: overlay?.lineGeoJson) { source in
            let layer = MLNLineStyleLayer(identifier: lineLayerId, source: source)
            layer.lineCap = NSExpression(forConstantValue: "round")
            layer.lineJoin = NSExpression(forConstantValue: "round")
            layer.lineColor = MapExpressions.levelColorStep(maxLevel: RunvealDong.maxLevel) { level in
                UIColor(RunvealDong.style(level: level).stroke)
            }
            layer.lineWidth = NSExpression(forConstantValue: lineWidth)
            if let minimumZoomLevel { layer.minimumZoomLevel = Float(minimumZoomLevel) }
            layer.maximumZoomLevel = Float(maximumZoomLevel)
            style.addLayer(layer)
        }

        MapSourceSupport.upsertShapeSource(style: style, sourceId: labelSourceId, geoJson: overlay?.labelGeoJson) { source in
            let layer = MLNSymbolStyleLayer(identifier: labelLayerId, source: source)
            layer.text = MapExpressions.nameAttribute
            layer.textFontNames = NSExpression(forConstantValue: labelFontNames)
            layer.textFontSize = NSExpression(forConstantValue: labelSize)
            layer.textColor = NSExpression(forConstantValue: colors.sigunguLabel)
            if allowsLabelOverlap {
                layer.textAllowsOverlap = NSExpression(forConstantValue: true)
                layer.textIgnoresPlacement = NSExpression(forConstantValue: true)
            } else {
                // 겹치는 라벨은 MapLibre가 숨긴다(기본값) — 살아남을 쪽을 탐험률로 정한다.
                layer.symbolSortKey = MapExpressions.labelSortKeyByPercent
            }
            if let minimumZoomLevel { layer.minimumZoomLevel = Float(minimumZoomLevel) }
            layer.maximumZoomLevel = Float(maximumZoomLevel)
            style.addLayer(layer)
        }
    }
}
