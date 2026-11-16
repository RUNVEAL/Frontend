import MapLibre

/// 기본 스타일(OpenFreeMap dark)에 내장된 지명/도로 이름 라벨과 행정구역 경계선은 어느 줌에서든
/// 우리 자체 라벨·테두리와 겹쳐 보이므로 모든 줌에서 항상 숨긴다. 스타일이 로드된 직후 한 번만
/// 호출하면 된다.
enum BaseStyleLabels {
    static func hide(style: MLNStyle) {
        let ids = RunvealMapConfig.baseStylePlaceLabelLayers
            + RunvealMapConfig.baseStyleRoadLabelLayers
            + RunvealMapConfig.baseStyleBoundaryLayers
        for id in ids {
            style.layer(withIdentifier: id)?.isVisible = false
        }
    }
}

/// 동 경계가 주인공인 뷰(`ZOOM_IN_THRESHOLD` 미만)에서는 베이스 스타일의 도로/수역/녹지 등을 흐리게
/// 깔아 우리 경계/라벨이 더 도드라지게 한다. `ZOOM_IN_THRESHOLD` 이상(러닝 포커스)에서는 원래
/// 밝기로 돌아와 도로가 또렷하게 보인다.
///
/// 스타일이 막 로드된 직후의 레이어 스냅샷만 대상으로 하므로, 그 뒤에 우리가 추가하는 동/시군구/시도
/// 레이어는 자동으로 제외된다 — 그래서 다른 레이어를 추가하기 전에 가장 먼저 호출해야 한다.
enum BaseMapDimmer {
    private static let lineOpacityDimmed: Double = 0.4
    private static let fillOpacityDimmed: Double = 0.3

    /// 간선/고속도로는 일반 도로(0.4)보다 더 흐리게 — 두꺼운 선이라 0.4로도 과하게 두드러져서.
    private static let majorRoadLineOpacityDimmed: Double = 0.15

    static func applyAtStyleLoad(style: MLNStyle) {
        let fullBrightnessAtZoom = RunvealMapConfig.zoomInThreshold
        for layer in style.layers {
            if let line = layer as? MLNLineStyleLayer {
                line.lineOpacity = MapExpressions.numberStep(
                    switchZoom: fullBrightnessAtZoom,
                    below: lineOpacityDimmed,
                    atOrAbove: 1
                )
            } else if let fill = layer as? MLNFillStyleLayer {
                fill.fillOpacity = MapExpressions.numberStep(
                    switchZoom: fullBrightnessAtZoom,
                    below: fillOpacityDimmed,
                    atOrAbove: 1
                )
            }
        }
        // 일반 도로 딤 처리 위에 간선/고속도로만 한 번 더 낮춘 값으로 덮어쓴다.
        for id in RunvealMapConfig.baseStyleMajorRoadLayers {
            guard let line = style.layer(withIdentifier: id) as? MLNLineStyleLayer else { continue }
            line.lineOpacity = MapExpressions.numberStep(
                switchZoom: fullBrightnessAtZoom,
                below: majorRoadLineOpacityDimmed,
                atOrAbove: 1
            )
        }
    }
}

/// 러닝 포커스(zoom >= `ZOOM_IN_THRESHOLD`)에서는 기본 다크 스타일의 건물/공원/숲 색이 배경(거의 검정)과
/// 구분이 안 가는 문제를 보정값으로 고친다. 그 아래 줌(동/시군구/시도 뷰)은 원래 스타일 색 그대로 둔다.
enum BaseStyleDarkCorrection {
    /// 원래 다크 스타일 색(`rgb(10,10,10)`).
    private static let buildingOriginal = UIColor(red: 10 / 255, green: 10 / 255, blue: 10 / 255, alpha: 1)
    /// 원래 다크 스타일 색(`rgb(32,32,32)`) — landuse_park/landcover_wood가 공유한다.
    private static let parkWoodOriginal = UIColor(red: 32 / 255, green: 32 / 255, blue: 32 / 255, alpha: 1)
    /// 네이버 지도 다크모드 실제 색을 픽셀 샘플링해 가져온 보정 색(`#34343A`).
    private static let buildingDarkFix = UIColor(red: 0x34 / 255, green: 0x34 / 255, blue: 0x3A / 255, alpha: 1)
    /// 구글 탭 다크 스타일의 poi.park 색과 맞춘 보정 색(`#1F2A1F`) — park/wood/grass가 공유한다.
    private static let parkWoodGrassDarkFix = UIColor(red: 0x1F / 255, green: 0x2A / 255, blue: 0x1F / 255, alpha: 1)

    static func applyAtStyleLoad(style: MLNStyle) {
        let switchZoom = RunvealMapConfig.zoomInThreshold

        if let building = style.layer(withIdentifier: RunvealMapConfig.layerBaseBuilding) as? MLNFillStyleLayer {
            building.fillColor = MapExpressions.colorStep(
                switchZoom: switchZoom,
                below: buildingOriginal,
                atOrAbove: buildingDarkFix
            )
        }
        for id in [RunvealMapConfig.layerBaseLandusePark, RunvealMapConfig.layerBaseLandcoverWood] {
            guard let fill = style.layer(withIdentifier: id) as? MLNFillStyleLayer else { continue }
            fill.fillColor = MapExpressions.colorStep(
                switchZoom: switchZoom,
                below: parkWoodOriginal,
                atOrAbove: parkWoodGrassDarkFix
            )
        }
        addGrassLayerIfMissing(style: style, switchZoom: switchZoom)
    }

    /// 다크 스타일엔 `class == grass` landcover 레이어가 아예 없다(liberty 스타일에만 있음) — 잔디 녹지가
    /// 러닝 포커스에서 안 보이던 문제라, liberty의 같은 레이어 정의를 가져와 다크 보정 색으로 새로
    /// 추가한다. `landuse_park` 바로 위에 꽂고, 러닝 포커스 전용이라 그 밑 줌에서는 아예 그리지 않는다.
    private static func addGrassLayerIfMissing(style: MLNStyle, switchZoom: Double) {
        let grassLayerId = RunvealMapConfig.layerBaseLandcoverGrass
        guard style.layer(withIdentifier: grassLayerId) == nil else { return }
        guard let source = style.source(withIdentifier: RunvealMapConfig.sourceBaseStyle) else { return }

        let layer = MLNFillStyleLayer(identifier: grassLayerId, source: source)
        layer.sourceLayerIdentifier = "landcover"
        layer.predicate = NSPredicate(format: "%K == %@", "class", "grass")
        layer.fillColor = NSExpression(forConstantValue: parkWoodGrassDarkFix)
        layer.minimumZoomLevel = Float(switchZoom)

        if let park = style.layer(withIdentifier: RunvealMapConfig.layerBaseLandusePark) {
            style.insertLayer(layer, above: park)
        } else {
            style.addLayer(layer)
        }
    }
}

/// 홈 줌인 뷰에서 "안 가본 길"을 고른 동안, 베이스 스타일의 도로를 디자인 토큰
/// `map.roadUnexploredFocus`(neutral/400) 한 색으로 덮어 강조한다. 다크 스타일의 도로는 원래 거의
/// 검정이라, 가본 길(금색 선)이 어디까지 뻗어 있는지는 보여도 "아직 안 간 길"이 어디로 이어지는지는
/// 알아보기 어렵다. 가본 길 레이어([VisitedRoadLayer])는 건드리지 않는다.
///
/// 토글을 끄면 원래 색으로 되돌려야 하므로, 스타일이 로드된 직후의 `lineColor`를 한 번 기억해 둔다
/// ([rememberOriginalColors]) — 반드시 [apply]보다 먼저 호출해야 한다.
final class BaseStyleRoadFocus {
    private var originalColors: [String: NSExpression] = [:]

    /// 스타일 로드 직후, 아직 아무도 도로 색을 건드리지 않은 상태에서 한 번 호출한다.
    func rememberOriginalColors(style: MLNStyle) {
        originalColors.removeAll()
        for id in RunvealMapConfig.baseStyleRoadLayers {
            guard let line = style.layer(withIdentifier: id) as? MLNLineStyleLayer,
                  let color = line.lineColor else { continue }
            originalColors[id] = color
        }
    }

    /// [focusColor]가 nil이면 [rememberOriginalColors]로 기억해 둔 원래 색으로 되돌린다.
    func apply(style: MLNStyle, focusColor: UIColor?) {
        for id in RunvealMapConfig.baseStyleRoadLayers {
            guard let line = style.layer(withIdentifier: id) as? MLNLineStyleLayer else { continue }
            if let focusColor {
                line.lineColor = NSExpression(forConstantValue: focusColor)
            } else if let original = originalColors[id] {
                line.lineColor = original
            }
        }
    }
}
