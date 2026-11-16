import SwiftUI

/// 구역 제목 + 정렬 드롭다운. 정렬을 누르면 바텀시트로 정렬 옵션을 고른다.
struct RunvealSectionHeader: View {
    private let title: String
    private let sortLabel: String?
    private let onSortClick: () -> Void

    init(title: String, sortLabel: String? = nil, onSortClick: @escaping () -> Void = {}) {
        self.title = title
        self.sortLabel = sortLabel
        self.onSortClick = onSortClick
    }

    var body: some View {
        let colors = RunvealTheme.colors
        HStack {
            Text(title)
                .runvealTextStyle(RunvealTheme.typography.bodyLSemibold)
                .foregroundStyle(colors.text.primary)
            Spacer()
            if let sortLabel {
                Button(action: onSortClick) {
                    HStack(spacing: RunvealSpacing.space4) {
                        Text(sortLabel)
                            .runvealTextStyle(RunvealTheme.typography.bodyMSemibold)
                            .foregroundStyle(colors.text.primary)
                        RunvealIcon(RunvealIcons.expand, tint: colors.icon.primary, size: 16)
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 24)
    }
}

#Preview {
    RunvealSectionHeader(title: "성수동", sortLabel: "거리순") {}
        .padding()
        .background(RunvealTheme.colors.bg.canvas)
}
