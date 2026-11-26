import SwiftUI

enum RunvealToastType { case `default`, error }

/// 토스트. 불투명 배경(글래스 아님), 두 줄까지. 화면에 띄우고 3초 후 사라지게 하는 타이머는
/// 호출하는 쪽이 맡는다.
///
/// **너비는 주어진 폭을 꽉 채운다**(시안 `Overlay/Toast`: "화면 좌우 16"은 마진이고 기본이 전체 폭).
/// 감싸는 쪽이 좌우 `RunvealLayout.marginScreen`만 주면 된다 — 여기서 채우지 않으면 문구 길이에 따라
/// 너비가 들쭉날쭉해진다(Android `RunvealToast`와 같다).
struct RunvealToast: View {
    private let message: String
    private let type: RunvealToastType

    init(message: String, type: RunvealToastType = .default) {
        self.message = message
        self.type = type
    }

    var body: some View {
        let colors = RunvealTheme.colors
        HStack(spacing: RunvealSpacing.space12) {
            if type == .error {
                RunvealIcon(RunvealIcons.alert, tint: colors.status.error, size: RunvealLayout.sizeIcon)
            }
            Text(message)
                .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                .foregroundStyle(colors.text.primary)
                .lineLimit(2)
                .truncationMode(.tail)
        }
        .padding(.vertical, RunvealSpacing.space12)
        .padding(.horizontal, RunvealSpacing.space16)
        // 여백을 먼저 주고 그 다음에 늘린다 — 순서가 반대면 배경이 여백 바깥까지 번진다.
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(colors.surface.raised, in: RunvealShapes.radius16)
        .overlay { RunvealShapes.radius16.stroke(colors.border.subtle, lineWidth: 1) }
    }
}

#Preview {
    VStack(spacing: 12) {
        RunvealToast(message: "코스를 저장했어요")
        RunvealToast(message: "코스를 저장하지 못했어요", type: .error)
    }
    .padding()
    .background(RunvealTheme.colors.bg.canvas)
}
