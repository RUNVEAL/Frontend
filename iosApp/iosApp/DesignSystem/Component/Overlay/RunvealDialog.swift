import SwiftUI

/// Default=확인 Gold, Destructive=확인 Danger.
enum RunvealDialogType { case `default`, destructive }

/// 확인 다이얼로그. 뒤 bg/scrim은 호출하는 쪽의 다이얼로그 호스트가 맡는다 —
/// 이 뷰는 다이얼로그 카드 모양만 그린다.
struct RunvealDialog: View {
    private let title: String
    private let confirmLabel: String
    private let cancelLabel: String
    private let description: String?
    private let type: RunvealDialogType
    private let onConfirm: () -> Void
    private let onCancel: () -> Void

    init(
        title: String,
        confirmLabel: String,
        cancelLabel: String,
        description: String? = nil,
        type: RunvealDialogType = .default,
        onConfirm: @escaping () -> Void,
        onCancel: @escaping () -> Void
    ) {
        self.title = title
        self.confirmLabel = confirmLabel
        self.cancelLabel = cancelLabel
        self.description = description
        self.type = type
        self.onConfirm = onConfirm
        self.onCancel = onCancel
    }

    var body: some View {
        let colors = RunvealTheme.colors
        VStack(alignment: .leading, spacing: RunvealSpacing.space16) {
            VStack(alignment: .leading, spacing: RunvealSpacing.space8) {
                Text(title)
                    .runvealTextStyle(RunvealTheme.typography.heading3)
                    .foregroundStyle(colors.text.primary)
                if let description {
                    Text(description)
                        .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                        .foregroundStyle(colors.text.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            .padding(RunvealSpacing.space8)

            HStack(spacing: RunvealSpacing.space8) {
                RunvealButton(label: cancelLabel, style: .secondary, fillWidth: true, onClick: onCancel)
                RunvealButton(
                    label: confirmLabel,
                    style: type == .destructive ? .danger : .gold,
                    fillWidth: true,
                    onClick: onConfirm
                )
            }
        }
        .padding(RunvealSpacing.space16)
        .frame(width: RunvealLayout.sizeDialogWidth)
        // 시안(Overlay/Dialog)이 주는 색을 그대로 쓴다 — surface/default(#272626 50%) +
        // border/default(#443F3D). `runvealGlass`를 쓰지 않는 건 그쪽이 배경 블러가 없는 걸
        // 메우려고 불투명도를 올린 fallback이어서다. 다이얼로그는 뒤에 스크림이 깔려서
        // 50%로도 글자가 묻히지 않는다(Android와 같은 판단).
        .background(colors.surface.default, in: RunvealShapes.radius24)
        .overlay { RunvealShapes.radius24.stroke(colors.border.default, lineWidth: 1) }
        .clipShape(RunvealShapes.radius24)
    }
}

#Preview {
    RunvealDialog(
        title: "코스를 삭제할까요?",
        confirmLabel: "삭제",
        cancelLabel: "취소",
        description: "삭제하면 되돌릴 수 없어요",
        type: .destructive,
        onConfirm: {},
        onCancel: {}
    )
    .padding()
    .background(RunvealTheme.colors.bg.canvas)
}
