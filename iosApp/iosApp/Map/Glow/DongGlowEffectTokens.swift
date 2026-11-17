import SharedLogic
import SwiftUI
import UIKit

/// `RunvealEffects.dong1`~`dong6`(빛)과 `RunvealLayout.mapDongLevelStrokeWidth0`~`6`(테두리 두께)
/// 토큰을 sharedLogic의 [GlowEffect]/px 숫자로 바꾼 것 — Android `DongGlowEffects`에 해당한다.
///
/// 값은 전부 토큰에서 읽으며 여기에 다시 적지 않는다. 인덱스 0은 0단계(빛 없음)용 빈 값.
enum DongGlowEffectTokens {

    static let effectsByLevel: [[GlowEffect]] = [
        [],
        RunvealEffects.dong1,
        RunvealEffects.dong2,
        RunvealEffects.dong3,
        RunvealEffects.dong4,
        RunvealEffects.dong5,
        RunvealEffects.dong6,
    ].map { shadows in shadows.map(glowEffect(from:)) }

    static let strokeWidthByLevel: [CGFloat] = [
        RunvealLayout.mapDongLevelStrokeWidth0,
        RunvealLayout.mapDongLevelStrokeWidth1,
        RunvealLayout.mapDongLevelStrokeWidth2,
        RunvealLayout.mapDongLevelStrokeWidth3,
        RunvealLayout.mapDongLevelStrokeWidth4,
        RunvealLayout.mapDongLevelStrokeWidth5,
        RunvealLayout.mapDongLevelStrokeWidth6,
    ]

    /// 캐시 키에 들어가는 토큰 요약 — 토큰 값이 하나라도 바뀌면 문자열이 달라져 옛 캐시가 무효화된다.
    static var tokenFingerprint: String {
        var parts: [String] = []
        for effects in effectsByLevel {
            for effect in effects {
                parts.append("\(effect.r),\(effect.g),\(effect.b),\(effect.alpha),\(effect.blur),\(effect.inset)")
            }
            parts.append(";")
        }
        parts.append(strokeWidthByLevel.map { "\($0)" }.joined(separator: ","))
        return parts.joined(separator: "|")
    }

    private static func glowEffect(from shadow: RunvealShadow) -> GlowEffect {
        let rgba = components(of: shadow.color)
        return GlowEffect(
            r: Float(rgba.red),
            g: Float(rgba.green),
            b: Float(rgba.blue),
            alpha: Float(rgba.alpha),
            // blur는 pt가 아니라 피그마 px 숫자 그대로(화면 배율 미적용) — Android와 같다.
            blur: Float(shadow.blur),
            inset: shadow.inset
        )
    }

    private static func components(of color: Color) -> (red: CGFloat, green: CGFloat, blue: CGFloat, alpha: CGFloat) {
        var red: CGFloat = 0
        var green: CGFloat = 0
        var blue: CGFloat = 0
        var alpha: CGFloat = 0
        UIColor(color).getRed(&red, green: &green, blue: &blue, alpha: &alpha)
        return (red, green, blue, alpha)
    }
}
