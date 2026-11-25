import SwiftUI

/**
 월 선택 패널(디자인 `Overlay/Month Picker`) — 연도 한 줄 + 12달 격자(4칸 x 3줄).
 Android `MonthPicker`.

 연도는 패널이 직접 들고 넘긴다(화살표로 넘겨 봐도 고르기 전까지는 목록이 안 바뀐다) — 고른 달만
 [onSelect]로 올린다. 띄우고 닫는 건 호출부가 맡는다.
 */
struct MonthPicker: View {
    let selectedYear: Int
    let selectedMonth: Int
    let onSelect: (Int, Int) -> Void

    /// 시안 값 — 패널 폭, 연도 줄 높이, 달 한 칸 크기.
    private static let width: CGFloat = 254
    private static let yearRowHeight: CGFloat = 32
    private static let monthCellWidth: CGFloat = 52
    private static let monthCellHeight: CGFloat = 36
    private static let monthsPerRow = 4

    /// 패널이 지금 보여주는 연도 — 고르기 전까지는 [selectedYear]와 달라질 수 있다.
    @State private var shownYear: Int

    init(selectedYear: Int, selectedMonth: Int, onSelect: @escaping (Int, Int) -> Void) {
        self.selectedYear = selectedYear
        self.selectedMonth = selectedMonth
        self.onSelect = onSelect
        _shownYear = State(initialValue: selectedYear)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: RunvealSpacing.space4) {
            yearRow
            ForEach(Array(stride(from: 1, through: 12, by: Self.monthsPerRow)), id: \.self) { firstMonth in
                HStack(spacing: RunvealSpacing.space4) {
                    ForEach(firstMonth..<(firstMonth + Self.monthsPerRow), id: \.self) { month in
                        monthCell(month)
                    }
                }
            }
        }
        .padding(RunvealSpacing.space16)
        .frame(width: Self.width)
        .background(RunvealTheme.colors.surface.raised)
        .clipShape(RunvealShapes.radius16)
        .overlay {
            RunvealShapes.radius16.stroke(RunvealTheme.colors.border.default, lineWidth: 1)
        }
    }

    private var yearRow: some View {
        HStack(spacing: 0) {
            yearArrow(pointsLeft: true, label: TabRecordStrings.previousYear) { shownYear -= 1 }
            Spacer()
            Text(TabRecordStrings.yearLabel(shownYear))
                .runvealTextStyle(RunvealTheme.typography.bodyMSemibold)
                .foregroundStyle(RunvealTheme.colors.text.primary)
            Spacer()
            yearArrow(pointsLeft: false, label: TabRecordStrings.nextYear) { shownYear += 1 }
        }
        .frame(maxWidth: .infinity)
        .frame(height: Self.yearRowHeight)
    }

    /// 고른 달만 채워 그린다(밝은 면 + 어두운 글자). 나머지는 테두리만 — **모서리는 열두 칸이
    /// 모두 radius/8이다**(시안 `Overlay/Month Picker`). Android는 선택 안 된 칸을 각지게 두는데
    /// 시안과 다르다.
    private func monthCell(_ month: Int) -> some View {
        let colors = RunvealTheme.colors
        let selected = shownYear == selectedYear && month == selectedMonth
        return Text(TabRecordStrings.monthLabel(month))
            .runvealTextStyle(RunvealTheme.typography.bodyMSemibold)
            .foregroundStyle(selected ? colors.text.onInverse : colors.text.primary)
            .frame(width: Self.monthCellWidth, height: Self.monthCellHeight)
            .background {
                if selected {
                    RunvealShapes.radius8.fill(colors.surface.inverse)
                }
            }
            .overlay {
                RunvealShapes.radius8.stroke(colors.border.default, lineWidth: 1)
            }
            .contentShape(Rectangle())
            .onTapGesture { onSelect(shownYear, month) }
    }

    /// 연도를 넘기는 화살표. chevron 아이콘 하나를 좌우로 돌려 쓴다(왼쪽용 아이콘이 따로 없다).
    private func yearArrow(pointsLeft: Bool, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            RunvealIcon(RunvealIcons.chevron, tint: RunvealTheme.colors.icon.primary)
                .rotationEffect(.degrees(pointsLeft ? 180 : 0))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}
