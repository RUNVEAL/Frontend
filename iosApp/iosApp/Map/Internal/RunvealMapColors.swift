import SwiftUI
import UIKit

/// `RunvealTheme.colors`에서 뽑아낸 색을 MapLibre 스타일 레이어용 `UIColor`로 미리 바꿔 둔 값 —
/// Android `MapColors`에 해당한다(Android는 Composable 컨텍스트 밖에서 쓰려고 ARGB `Int`로 바꿔 둔다).
struct RunvealMapColors {
    let visitedRoadGlow: UIColor
    let visitedRoadCore: UIColor
    let currentLocation: UIColor
    let dongLabelPrimary: UIColor
    let dongLabelSecondary: UIColor
    let sigunguLabel: UIColor
    let recommendedRouteAlt: UIColor
    let recommendedRouteSelectedGlow: UIColor
    let recommendedRouteSelected: UIColor
    let courseGuide: UIColor
    /// 러닝 중 실시간 선 — 이미 가본 구간(흰색, glow 없음).
    let liveRunOld: UIColor
    /// 러닝 중 실시간 선 — 처음 가보는 구간(금색 코어 + glow).
    let liveRunNew: UIColor
    let liveRunNewGlow: UIColor
    /// "안 가본 길" 보기에서 베이스 지도 도로를 덮는 색.
    let roadUnexploredFocus: UIColor

    static let `default` = RunvealMapColors(colors: RunvealTheme.colors)

    init(colors: RunvealColors) {
        visitedRoadGlow = UIColor(colors.accent.glow)
        visitedRoadCore = UIColor(colors.map.roadExplored)
        currentLocation = UIColor(colors.map.location)
        dongLabelPrimary = UIColor(colors.text.primary)
        dongLabelSecondary = UIColor(colors.text.secondary)
        sigunguLabel = UIColor(colors.text.secondary)
        recommendedRouteAlt = UIColor(colors.map.routeAlt)
        recommendedRouteSelectedGlow = UIColor(colors.map.roadNewHalo)
        recommendedRouteSelected = UIColor(colors.map.roadNew)
        courseGuide = UIColor(colors.text.primary)
        liveRunOld = UIColor(colors.map.roadToday)
        liveRunNew = UIColor(colors.map.roadNew)
        liveRunNewGlow = UIColor(colors.map.roadNewHalo)
        roadUnexploredFocus = UIColor(colors.map.roadUnexploredFocus)
    }
}
