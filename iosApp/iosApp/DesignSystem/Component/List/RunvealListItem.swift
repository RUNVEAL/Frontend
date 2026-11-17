import SwiftUI

enum RunvealListItemSize { case l, m }
enum RunvealListItemTrailing { case none, chevron, textButton }

/// 단순한 리스트 줄. 줄 전체가 터치 영역. [leadingIcon]/[leadingCheckbox]/[leadingNumber] 중
/// 하나만 쓴다(없으면 왼쪽 여백 없음).
struct RunvealListItem: View {
    private let title: String
    private let description: String?
    private let leadingIcon: RunvealIconSpec?
    private let leadingCheckbox: RunvealCheckboxState?
    private let leadingNumber: String?
    private let size: RunvealListItemSize
    private let trailing: RunvealListItemTrailing
    private let trailingButtonLabel: String?
    private let onTrailingButtonClick: () -> Void
    private let showDivider: Bool
    private let onClick: () -> Void

    init(
        title: String,
        description: String? = nil,
        leadingIcon: RunvealIconSpec? = nil,
        leadingCheckbox: RunvealCheckboxState? = nil,
        leadingNumber: String? = nil,
        size: RunvealListItemSize = .m,
        trailing: RunvealListItemTrailing = .none,
        trailingButtonLabel: String? = nil,
        onTrailingButtonClick: @escaping () -> Void = {},
        showDivider: Bool = false,
        onClick: @escaping () -> Void
    ) {
        self.title = title
        self.description = description
        self.leadingIcon = leadingIcon
        self.leadingCheckbox = leadingCheckbox
        self.leadingNumber = leadingNumber
        self.size = size
        self.trailing = trailing
        self.trailingButtonLabel = trailingButtonLabel
        self.onTrailingButtonClick = onTrailingButtonClick
        self.showDivider = showDivider
        self.onClick = onClick
    }

    var body: some View {
        let colors = RunvealTheme.colors
        let minHeight: CGFloat = size == .l ? 64 : 52
        let paddingVertical: CGFloat = size == .l ? RunvealSpacing.space12 : RunvealSpacing.space2

        VStack(spacing: 0) {
            HStack(spacing: RunvealSpacing.space12) {
                leading

                VStack(alignment: .leading, spacing: 0) {
                    Text(title)
                        .runvealTextStyle(
                            size == .l
                                ? RunvealTheme.typography.bodyLSemibold
                                : RunvealTheme.typography.bodyMRegular
                        )
                        .foregroundStyle(size == .l ? colors.text.primary : colors.text.secondary)
                    if let description {
                        Text(description)
                            .runvealTextStyle(RunvealTheme.typography.captionMRegular)
                            .foregroundStyle(colors.text.tertiary)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                switch trailing {
                case .chevron:
                    RunvealIcon(RunvealIcons.chevron, tint: colors.icon.tertiary, size: RunvealLayout.sizeIcon)
                case .textButton:
                    RunvealButton(
                        label: trailingButtonLabel ?? "",
                        style: .text,
                        onClick: onTrailingButtonClick
                    )
                case .none:
                    EmptyView()
                }
            }
            .padding(.vertical, paddingVertical)
            .frame(maxWidth: .infinity, minHeight: minHeight)
            .contentShape(Rectangle())
            .onTapGesture(perform: onClick)

            if showDivider {
                Rectangle().fill(colors.border.subtle).frame(height: 1)
            }
        }
    }

    @ViewBuilder
    private var leading: some View {
        let colors = RunvealTheme.colors
        if let leadingIcon {
            RunvealIcon(leadingIcon, tint: colors.icon.primary, size: RunvealLayout.sizeIcon)
        } else if let leadingCheckbox {
            RunvealCheckbox(state: leadingCheckbox)
        } else if let leadingNumber {
            Text(leadingNumber)
                .runvealTextStyle(RunvealTheme.typography.bodyLSemibold)
                .foregroundStyle(colors.text.accent)
        }
    }
}

#Preview {
    VStack(spacing: 0) {
        RunvealListItem(
            title: "코스 이름",
            description: "성수동 · 5.21km",
            leadingIcon: RunvealIcons.course,
            size: .l,
            trailing: .chevron,
            showDivider: true
        ) {}
        RunvealListItem(title: "약관에 동의합니다", leadingCheckbox: .checked) {}
    }
    .padding()
    .background(RunvealTheme.colors.bg.canvas)
}
