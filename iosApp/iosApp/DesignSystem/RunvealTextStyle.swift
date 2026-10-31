import SwiftUI

/// Figma 타이포그래피 한 묶음. RunvealTypography(자동 생성)의 각 항목이 이 타입이다.
///
/// `lineHeight`는 Figma의 줄 높이(px)를 그대로 담고 있지만, SwiftUI의 `.lineSpacing(_:)`는
/// "줄 사이 추가 간격"이라 Compose의 lineHeight(줄 전체 높이)와 의미가 다르다.
/// 정확히 맞추려면 UIKit(NSAttributedString.lineHeightMultiple 등) 기반 구현이 필요해 지금은
/// font + tracking만 적용한다. 줄 간격까지 정확히 맞춰야 하는 화면에서는 `lineHeight`를 직접 참고한다.
struct RunvealTextStyle {
    let font: Font
    let lineHeight: CGFloat
    let tracking: CGFloat
}

extension Text {
    /// `Text("37").runvealTextStyle(RunvealTheme.typography.numDisplay)`
    func runvealTextStyle(_ style: RunvealTextStyle) -> Text {
        self.font(style.font).tracking(style.tracking)
    }
}
