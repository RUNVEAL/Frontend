import SwiftUI

/// 동 하나를 그릴 때 필요한 스타일 묶음.
struct DongLevelStyle {
    let level: Int
    let stroke: Color
    let fill: Color
    let strokeWidth: CGFloat
    let glow: [RunvealShadow]
}

/// 동 탐험률 단계. 값은 모두 자동 생성 토큰(RunvealColors.map.dong, RunvealLayout, RunvealEffects)에서 가져온다.
/// 밝기는 탐험률만 뜻한다. 내 동네는 단계 스타일 그대로, 테두리만 mapDongLevelMineStrokeWidth.
///
/// | 단계 | 범위 |
/// |---|---|
/// | 0 | 0% (미탐험) |
/// | 1 | 1~4% |
/// | 2 | 5~9% |
/// | 3 | 10~19% |
/// | 4 | 20~39% |
/// | 5 | 40~69% |
/// | 6 | 70~100% |
enum RunvealDong {
    static let maxLevel = 6

    /// 정수 탐험률(%)을 단계(0..6)로 변환
    static func levelOf(_ ratePercent: Int) -> Int {
        switch ratePercent {
        case ..<1: return 0
        case ..<5: return 1
        case ..<10: return 2
        case ..<20: return 3
        case ..<40: return 4
        case ..<70: return 5
        default: return 6
        }
    }

    static func style(level: Int, colors: RunvealColors = RunvealTheme.colors, isMine: Bool = false) -> DongLevelStyle {
        let l = min(max(level, 0), maxLevel)
        let dong = colors.map.dong
        let stroke = [dong.stroke0, dong.stroke1, dong.stroke2, dong.stroke3, dong.stroke4, dong.stroke5, dong.stroke6]
        let fill = [Color.clear, dong.fill1, dong.fill2, dong.fill3, dong.fill4, dong.fill5, dong.fill6]
        let width: [CGFloat] = [
            RunvealLayout.mapDongLevelStrokeWidth0, RunvealLayout.mapDongLevelStrokeWidth1,
            RunvealLayout.mapDongLevelStrokeWidth2, RunvealLayout.mapDongLevelStrokeWidth3,
            RunvealLayout.mapDongLevelStrokeWidth4, RunvealLayout.mapDongLevelStrokeWidth5,
            RunvealLayout.mapDongLevelStrokeWidth6,
        ]
        let glow: [[RunvealShadow]] = [
            [], RunvealEffects.dong1, RunvealEffects.dong2, RunvealEffects.dong3,
            RunvealEffects.dong4, RunvealEffects.dong5, RunvealEffects.dong6,
        ]
        return DongLevelStyle(
            level: l,
            stroke: stroke[l],
            fill: fill[l],
            strokeWidth: isMine ? RunvealLayout.mapDongLevelMineStrokeWidth : width[l],
            glow: glow[l]
        )
    }
}
