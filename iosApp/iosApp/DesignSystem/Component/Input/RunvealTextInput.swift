import SwiftUI

/// 한 줄 입력(코스 이름 등). focused/filled 같은 시각 상태는 포커스·[text]로 자동 반영되고,
/// [isError]/[enabled]만 직접 전달한다.
struct RunvealTextInput: View {
    @Binding private var text: String
    private let label: String
    private let placeholder: String
    private let helper: String?
    private let isError: Bool
    private let enabled: Bool

    @FocusState private var focused: Bool

    init(
        text: Binding<String>,
        label: String,
        placeholder: String = "",
        helper: String? = nil,
        isError: Bool = false,
        enabled: Bool = true
    ) {
        self._text = text
        self.label = label
        self.placeholder = placeholder
        self.helper = helper
        self.isError = isError
        self.enabled = enabled
    }

    var body: some View {
        let colors = RunvealTheme.colors
        let borderColor: Color = {
            if isError { return colors.status.error }
            if focused { return colors.accent.default }
            return colors.border.default
        }()
        let borderWidth: CGFloat = (isError || focused) ? 1.5 : 1

        VStack(alignment: .leading, spacing: RunvealSpacing.space8) {
            Text(label)
                .runvealTextStyle(RunvealTheme.typography.captionMRegular)
                .foregroundStyle(colors.text.secondary)

            ZStack(alignment: .leading) {
                if text.isEmpty {
                    Text(placeholder)
                        .runvealTextStyle(RunvealTheme.typography.bodyLRegular)
                        .foregroundStyle(colors.text.tertiary)
                }
                TextField("", text: $text)
                    .textFieldStyle(.plain)
                    .font(RunvealTheme.typography.bodyLRegular.font)
                    .foregroundStyle(colors.text.primary)
                    .tint(colors.accent.default)
                    .focused($focused)
                    .disabled(!enabled)
            }
            .padding(.horizontal, RunvealSpacing.space16)
            .frame(height: RunvealLayout.sizeTextField)
            .background(colors.surface.default, in: RunvealShapes.radius16)
            .overlay { RunvealShapes.radius16.stroke(borderColor, lineWidth: borderWidth) }

            if let helper {
                Text(helper)
                    .runvealTextStyle(RunvealTheme.typography.captionMRegular)
                    .foregroundStyle(isError ? colors.status.error : colors.text.tertiary)
            }
        }
        .opacity(enabled ? 1 : 0.4)
    }
}

#Preview {
    @Previewable @State var empty = ""
    @Previewable @State var filled = "성수동 러닝"
    VStack(spacing: 16) {
        RunvealTextInput(text: $empty, label: "코스 이름", placeholder: "코스 이름을 입력하세요")
        RunvealTextInput(text: $filled, label: "코스 이름", helper: "이미 있는 이름이에요", isError: true)
    }
    .padding()
    .background(RunvealTheme.colors.bg.canvas)
}
