import SwiftUI

/// 여러 개를 각각 켜고 끄는 선택 표시(약관 동의 등). 탭 핸들러가 없다 — 단독으로 두지 말고
/// List Item(leading=checkbox) 안에서 줄 전체를 누르게 한다.
enum RunvealCheckboxState { case unchecked, mixed, checked }

struct RunvealCheckbox: View {
    private static let side: CGFloat = 20

    private let state: RunvealCheckboxState

    init(state: RunvealCheckboxState) {
        self.state = state
    }

    var body: some View {
        let colors = RunvealTheme.colors
        let shape = RoundedRectangle(cornerRadius: 4)

        ZStack {
            switch state {
            case .unchecked:
                shape.stroke(colors.border.strong, lineWidth: 1.5)
            case .mixed, .checked:
                shape.fill(colors.accent.default)
            }

            switch state {
            case .checked:
                CheckMark()
                    .stroke(
                        colors.icon.onAccent,
                        style: StrokeStyle(lineWidth: 2, lineCap: .round, lineJoin: .round)
                    )
                    .frame(width: 10, height: 8)
            case .mixed:
                Rectangle()
                    .fill(colors.icon.onAccent)
                    .frame(width: 8, height: 2)
            case .unchecked:
                EmptyView()
            }
        }
        .frame(width: Self.side, height: Self.side)
    }
}

/// Android의 Canvas 체크 표시와 같은 비율(0, 0.55) → (0.38, 1) → (1, 0).
private struct CheckMark: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: 0, y: rect.height * 0.55))
        path.addLine(to: CGPoint(x: rect.width * 0.38, y: rect.height))
        path.addLine(to: CGPoint(x: rect.width, y: 0))
        return path
    }
}

#Preview {
    HStack(spacing: 12) {
        RunvealCheckbox(state: .unchecked)
        RunvealCheckbox(state: .mixed)
        RunvealCheckbox(state: .checked)
    }
    .padding()
    .background(RunvealTheme.colors.bg.canvas)
}
