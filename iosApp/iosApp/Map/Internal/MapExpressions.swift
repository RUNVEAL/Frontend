import MapLibre
import SwiftUI

/// MapLibre iOS 스타일 식(`NSExpression`)을 만드는 헬퍼를 한곳에 모았다.
///
/// Android는 `Expression.step(...)`/`Expression.interpolate(...)` 빌더 DSL을 쓰지만 iOS SDK는
/// `NSExpression`의 전용 이니셜라이저를 쓴다. 그 이니셜라이저 이름은 MapLibre 메이저 버전에 따라
/// 바뀌어 왔으므로(Mapbox GL iOS의 `MGL*` → MapLibre의 `MLN*`), **SDK 이름에 기대는 코드는 이 파일에만**
/// 둔다 — 버전을 올려서 이름이 달라지면 여기만 고치면 된다.
enum MapExpressions {

    /// 줌에 따라 색이 계단식으로 바뀌는 식 — `zoom < switchZoom`이면 [below], 그 이상이면 [atOrAbove].
    static func colorStep(switchZoom: Double, below: UIColor, atOrAbove: UIColor) -> NSExpression {
        NSExpression(
            forMLNStepping: .zoomLevelVariable,
            from: NSExpression(forConstantValue: below),
            stops: NSExpression(forConstantValue: [switchZoom: atOrAbove])
        )
    }

    /// 줌에 따라 숫자가 계단식으로 바뀌는 식.
    static func numberStep(switchZoom: Double, below: Double, atOrAbove: Double) -> NSExpression {
        NSExpression(
            forMLNStepping: .zoomLevelVariable,
            from: NSExpression(forConstantValue: below),
            stops: NSExpression(forConstantValue: [switchZoom: atOrAbove])
        )
    }

    /// 피처의 숫자 속성(`level` 0~6)에 따라 색이 바뀌는 계단식 식.
    /// 레벨은 순서가 있는 값이라 타입에 민감한 `match`보다 `step`이 안전하다.
    static func levelColorStep(maxLevel: Int, colorForLevel: (Int) -> UIColor) -> NSExpression {
        var stops: [NSNumber: UIColor] = [:]
        for level in 1...maxLevel {
            stops[NSNumber(value: level)] = colorForLevel(level)
        }
        return NSExpression(
            forMLNStepping: NSExpression(forKeyPath: "level"),
            from: NSExpression(forConstantValue: colorForLevel(0)),
            stops: NSExpression(forConstantValue: stops)
        )
    }

    /// 줌 [zoomSmall]에서 [base] * [smallScale], 줌 [zoomBase]에서 [base]로 선형 보간하는 두께·블러 식.
    static func zoomInterpolated(
        base: Double,
        zoomSmall: Double,
        zoomBase: Double,
        smallScale: Double
    ) -> NSExpression {
        NSExpression(
            forMLNInterpolating: .zoomLevelVariable,
            curveType: .linear,
            parameters: nil,
            stops: NSExpression(forConstantValue: [
                zoomSmall: base * smallScale,
                zoomBase: base,
            ])
        )
    }

    static func constant(_ color: Color) -> NSExpression {
        NSExpression(forConstantValue: UIColor(color))
    }

    static func constant(_ value: Double) -> NSExpression {
        NSExpression(forConstantValue: value)
    }

    /// 동/시군구/시도 라벨 — `name` 속성을 그대로 보여준다.
    static var nameAttribute: NSExpression {
        NSExpression(forKeyPath: "name")
    }

    /// 동 라벨 — "이름⏎숫자%" 두 줄.
    ///
    /// Android는 `Expression.format`으로 토큰마다 색을 다르게 주지만, iOS 심볼 레이어는 `textColor`
    /// 하나만 쓸 수 있어서 글자 색을 나누지 못한다. 문자열 합치기도 스타일 식으로 하지 않고
    /// `IosDongOverlayBuilder`가 GeoJSON의 `label` 프로퍼티에 미리 두 줄로 만들어 둔 값을 쓴다 —
    /// 식으로 조립하면 SDK 버전마다 함수 이름이 달라 깨지기 쉽다.
    static var dongLabelText: NSExpression {
        NSExpression(forKeyPath: "label")
    }
}
