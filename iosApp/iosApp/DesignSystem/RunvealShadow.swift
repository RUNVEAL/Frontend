import SwiftUI

/// Figma 효과 한 겹. RunvealEffects(자동 생성)의 shadow 토큰이 이 타입의 배열이다.
/// blur는 Figma 값 그대로라 기기에서 눈으로 한 번 맞춰야 할 수 있다.
struct RunvealShadow {
    let color: Color
    let blur: CGFloat
    var offsetX: CGFloat = 0
    var offsetY: CGFloat = 0
    var spread: CGFloat = 0
    /// true면 안쪽 빛(inner shadow)
    var inset: Bool = false
    /// Figma LINEAR_DODGE = .plus
    var blendMode: BlendMode = .normal
}

extension View {
    /// 바깥 빛(inset = false)만 그린다.
    /// `someView.runvealDropShadow(RunvealEffects.glowGold)`
    func runvealDropShadow(_ layers: [RunvealShadow]) -> some View {
        layers.filter { !$0.inset }.reduce(AnyView(self)) { view, layer in
            AnyView(view.shadow(color: layer.color, radius: layer.blur, x: layer.offsetX, y: layer.offsetY))
        }
    }

    /// TODO: 진짜 inner shadow는 SwiftUI 기본 API로 불가 (Android는 Modifier.innerShadow로 구현됨).
    /// 현재는 no-op — UIKit CALayer 기반 구현이나 별도 라이브러리 도입 시 교체.
    func runvealInnerShadow(_ layers: [RunvealShadow]) -> some View {
        self
    }

    /// 글래스 표면 (현재는 블러 없는 fallback 스타일: glassFallbackSurface + border.subtle 1pt).
    ///
    /// TODO: 뒤 배경 블러(RunvealEffects.glassBlur)는 SwiftUI 기본 API로 불가.
    ///  iOS 15+의 `.background(.ultraThinMaterial)` 같은 Material 도입 시
    ///  surface.default + border.default + blur로 교체 검토. (Android도 같은 이유로 TODO 상태)
    func runvealGlass<S: Shape>(_ shape: S) -> some View {
        self
            .background(RunvealEffects.glassFallbackSurface, in: shape)
            .overlay(shape.stroke(RunvealTheme.colors.border.subtle, lineWidth: 1))
    }
}
