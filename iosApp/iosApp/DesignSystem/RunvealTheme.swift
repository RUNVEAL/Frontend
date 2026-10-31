import SwiftUI

/// Runveal 디자인 시스템 (다크 모드 하나).
///
/// 토큰 값은 design/runveal-tokens.json에서 빌드 때 자동 생성된다 (gradle/runveal-tokens-swift.gradle.kts,
/// iosApp.xcodeproj의 "Generate Runveal Design Tokens" Run Script 빌드 단계). JSON만 바꾸고
/// 다시 빌드하면 앱에 반영된다.
///
/// 사용: `RunvealTheme.colors.text.primary`, `RunvealTheme.typography.heading2`,
/// `RunvealSpacing.space16`, `RunvealShapes.radius16`, `RunvealLayout.marginScreen`
enum RunvealTheme {
    static let colors = RunvealColors()
    static let typography = RunvealTypography()
}
