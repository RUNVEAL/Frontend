import SwiftUI

/// 코스 조건 설정 바텀시트 — 거리(프리셋 + 직접입력 휠), 안 가본 길 우선, 러닝 필터.
struct FilterBottomSheet: View {
    let selectedDistance: String
    let isCustomDistanceSelected: Bool
    let customDistanceIndex: Int
    let avoidVisitedPreferred: Bool
    let selectedFilters: Set<String>
    let onDistanceSelect: (String) -> Void
    let onCustomDistanceChipClick: () -> Void
    let onCustomDistanceIndexChange: (Int) -> Void
    let onAvoidVisitedPreferredChange: (Bool) -> Void
    let onFilterToggle: (String) -> Void
    let onBack: () -> Void
    let onReset: () -> Void
    let onApply: () -> Void

    var body: some View {
        RunvealBottomSheet(
            header: {
                RunvealBottomSheetHeader(
                    type: .toolbar,
                    title: CourseRecommendStrings.filterSheetTitle,
                    showBack: true,
                    onBackClick: onBack,
                    actionLabel: CourseRecommendStrings.filterSheetReset,
                    onActionClick: onReset
                )
            },
            content: {
                content
            },
            footer: {
                RunvealButtonGroup {
                    RunvealButtonGroupSingle(label: CourseRecommendStrings.filterSheetApply, onClick: onApply)
                }
            }
        )
    }

    private var content: some View {
        let colors = RunvealTheme.colors
        return VStack(alignment: .leading, spacing: RunvealSpacing.space24) {
            VStack(alignment: .leading, spacing: RunvealSpacing.space8) {
                Text(CourseRecommendStrings.desiredDistance)
                    .runvealTextStyle(RunvealTheme.typography.bodyLSemibold)
                    .foregroundStyle(colors.text.primary)

                ChipWrap {
                    ForEach(CourseCreateOptions.distanceOptions, id: \.self) { option in
                        RunvealChipOption(
                            label: option,
                            selected: !isCustomDistanceSelected && option == selectedDistance
                        ) {
                            onDistanceSelect(option)
                        }
                    }
                    RunvealChipOption(
                        label: isCustomDistanceSelected
                            ? "\(CourseCreateOptions.customDistanceValues[customDistanceIndex])km"
                            : CourseRecommendStrings.customDistance,
                        selected: isCustomDistanceSelected,
                        onClick: onCustomDistanceChipClick
                    )
                }

                if isCustomDistanceSelected {
                    RunvealWheelPicker(
                        values: CourseCreateOptions.customDistanceValues,
                        selectedIndex: customDistanceIndex,
                        unit: "km",
                        onSelectedIndexChange: onCustomDistanceIndexChange
                    )
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            HStack(spacing: RunvealSpacing.space20) {
                VStack(alignment: .leading, spacing: RunvealSpacing.space4) {
                    Text(CourseRecommendStrings.avoidVisitedTitle)
                        .runvealTextStyle(RunvealTheme.typography.bodyLSemibold)
                        .foregroundStyle(colors.text.primary)
                    Text(CourseRecommendStrings.avoidVisitedDescription)
                        .runvealTextStyle(RunvealTheme.typography.captionMRegular)
                        .foregroundStyle(colors.text.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                RunvealToggleSwitch(checked: avoidVisitedPreferred, onCheckedChange: onAvoidVisitedPreferredChange)
            }
            .frame(maxWidth: .infinity)

            VStack(alignment: .leading, spacing: RunvealSpacing.space8) {
                Text(CourseRecommendStrings.runningFilter)
                    .runvealTextStyle(RunvealTheme.typography.bodyLSemibold)
                    .foregroundStyle(colors.text.primary)
                ChipWrap {
                    ForEach(CourseCreateOptions.filterOptions, id: \.self) { option in
                        RunvealChipOption(label: option, selected: selectedFilters.contains(option)) {
                            onFilterToggle(option)
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// 칩을 줄바꿈해서 배치한다 — Compose `FlowRow` 대응. iOS 16+ `Layout`으로 직접 구현한다
/// (SwiftUI에는 FlowRow에 해당하는 기본 레이아웃이 없다).
struct ChipWrap<Content: View>: View {
    private let spacing: CGFloat
    @ViewBuilder private let content: () -> Content

    init(spacing: CGFloat = RunvealSpacing.space8, @ViewBuilder content: @escaping () -> Content) {
        self.spacing = spacing
        self.content = content
    }

    var body: some View {
        FlowLayout(spacing: spacing) {
            content()
        }
    }
}

private struct FlowLayout: Layout {
    let spacing: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var rowWidth: CGFloat = 0
        var rowHeight: CGFloat = 0
        var totalHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if rowWidth > 0, rowWidth + spacing + size.width > maxWidth {
                totalHeight += rowHeight + spacing
                rowWidth = size.width
                rowHeight = size.height
            } else {
                rowWidth += rowWidth > 0 ? spacing + size.width : size.width
                rowHeight = max(rowHeight, size.height)
            }
        }
        totalHeight += rowHeight
        return CGSize(width: proposal.width ?? rowWidth, height: totalHeight)
    }

    func placeSubviews(
        in bounds: CGRect,
        proposal: ProposedViewSize,
        subviews: Subviews,
        cache: inout ()
    ) {
        var x = bounds.minX
        var y = bounds.minY
        var rowHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > bounds.minX, x + size.width > bounds.maxX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            subview.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}
