import SwiftUI

/// 켜고 끄는 스위치(안 가본 길 우선 추천 등). 터치 영역은 보통 행 전체가 받고, 비활성은 전체 40% 투명.
struct RunvealToggleSwitch: View {
    private let checked: Bool
    private let enabled: Bool
    private let onCheckedChange: (Bool) -> Void

    init(checked: Bool, enabled: Bool = true, onCheckedChange: @escaping (Bool) -> Void) {
        self.checked = checked
        self.enabled = enabled
        self.onCheckedChange = onCheckedChange
    }

    var body: some View {
        let colors = RunvealTheme.colors
        let trackShape = RoundedRectangle(cornerRadius: 16)
        let trackColor = checked ? colors.accent.default : colors.border.strong

        return Button {
            onCheckedChange(!checked)
        } label: {
            ZStack(alignment: .leading) {
                RunvealShapes.full
                    .fill(colors.surface.raised)
                    .frame(width: 26, height: 26)
                    .offset(x: checked ? 20 : 0)
            }
            .padding(3)
            .frame(width: 52, height: 32, alignment: .leading)
            .background(trackColor, in: trackShape)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.4)
        .animation(.easeInOut(duration: 0.15), value: checked)
        .accessibilityAddTraits(checked ? [.isButton, .isSelected] : .isButton)
    }
}

#Preview {
    @Previewable @State var on = true
    RunvealToggleSwitch(checked: on) { on = $0 }
        .padding()
        .background(RunvealTheme.colors.bg.canvas)
}
