import SwiftUI

enum RunvealBadgeSize { case l, m }
enum RunvealBadgeTone { case `default`, accent }

/// 누를 수 없는 강조 라벨. 라벨 + 값(선택). 예: 새로 탐험한 길 +0.8km.
struct RunvealBadge: View {
    private let label: String
    private let value: String?
    private let size: RunvealBadgeSize
    private let tone: RunvealBadgeTone

    init(
        label: String,
        value: String? = nil,
        size: RunvealBadgeSize = .m,
        tone: RunvealBadgeTone = .default
    ) {
        self.label = label
        self.value = value
        self.size = size
        self.tone = tone
    }

    var body: some View {
        let colors = RunvealTheme.colors
        let typography = size == .l ? RunvealTheme.typography.bodyMSemibold : RunvealTheme.typography.captionSSemibold
        let gap: CGFloat = size == .l ? RunvealSpacing.space8 : 6
        let paddingVertical: CGFloat = size == .l ? RunvealSpacing.space8 : 6
        let paddingHorizontal: CGFloat = size == .l ? RunvealSpacing.space12 : 10
        let labelColor = tone == .accent ? colors.text.accent : colors.text.secondary

        HStack(spacing: gap) {
            Text(label).runvealTextStyle(typography).foregroundStyle(labelColor)
            if let value {
                Text(value).runvealTextStyle(typography).foregroundStyle(colors.text.accent)
            }
        }
        .padding(.vertical, paddingVertical)
        .padding(.horizontal, paddingHorizontal)
        .background(colors.surface.raised, in: RunvealShapes.full)
    }
}

#Preview {
    HStack(spacing: 8) {
        RunvealBadge(label: "새로 탐험한 길", value: "+0.8km", size: .l)
        RunvealBadge(label: "새로 탐험한 길", value: "+0.8km", size: .m, tone: .accent)
    }
    .padding()
    .background(RunvealTheme.colors.bg.canvas)
}
