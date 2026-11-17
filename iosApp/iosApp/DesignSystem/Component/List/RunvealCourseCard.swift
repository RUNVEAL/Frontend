import SwiftUI

enum RunvealCourseCardState { case `default`, selected }

/// 추천 코스 카드. 가로 스크롤, 카드 간격 16. 선택된 카드는 테두리 accent/default + 이름 text/accent.
struct RunvealCourseCard: View {
    private static let width: CGFloat = 240

    private let name: String
    private let tags: [String]
    private let distanceValue: String
    private let distanceUnit: String
    private let timeValue: String
    private let timeUnit: String
    private let elevationValue: String
    private let elevationUnit: String
    private let newRoadValue: String
    private let newRoadUnit: String
    private let state: RunvealCourseCardState
    private let onClick: () -> Void

    init(
        name: String,
        tags: [String],
        distanceValue: String,
        distanceUnit: String,
        timeValue: String,
        timeUnit: String,
        elevationValue: String,
        elevationUnit: String,
        newRoadValue: String,
        newRoadUnit: String,
        state: RunvealCourseCardState = .default,
        onClick: @escaping () -> Void
    ) {
        self.name = name
        self.tags = tags
        self.distanceValue = distanceValue
        self.distanceUnit = distanceUnit
        self.timeValue = timeValue
        self.timeUnit = timeUnit
        self.elevationValue = elevationValue
        self.elevationUnit = elevationUnit
        self.newRoadValue = newRoadValue
        self.newRoadUnit = newRoadUnit
        self.state = state
        self.onClick = onClick
    }

    var body: some View {
        let colors = RunvealTheme.colors
        let borderColor = state == .selected ? colors.accent.default : colors.border.default
        let nameColor = state == .selected ? colors.text.accent : colors.text.primary

        return Button(action: onClick) {
            VStack(alignment: .leading, spacing: RunvealSpacing.space12) {
                HStack(spacing: RunvealSpacing.space8) {
                    Text(name)
                        .runvealTextStyle(RunvealTheme.typography.heading3)
                        .foregroundStyle(nameColor)
                    if !tags.isEmpty {
                        HStack(alignment: .bottom, spacing: RunvealSpacing.space4) {
                            ForEach(tags.prefix(2), id: \.self) { tag in
                                RunvealTag(label: tag)
                            }
                        }
                    }
                    Spacer(minLength: 0)
                    RunvealIcon(RunvealIcons.chevron, tint: colors.icon.tertiary, size: 16)
                }
                .frame(maxWidth: .infinity)

                // 수치가 없는 항목(고도, 새 길)도 공란으로 둘 뿐 칸을 차지하는 건 동일 — 같은 너비로
                // 나눠야 카드마다 칸 위치가 어긋나지 않는다.
                HStack(alignment: .top, spacing: RunvealSpacing.space12) {
                    RunvealStat(
                        value: distanceValue,
                        label: RunvealDesignSystemStrings.courseCardStatDistance,
                        unit: distanceUnit,
                        type: .small
                    )
                    .frame(maxWidth: .infinity, alignment: .leading)
                    RunvealStat(
                        value: timeValue,
                        label: RunvealDesignSystemStrings.courseCardStatTime,
                        unit: timeUnit,
                        type: .small
                    )
                    .frame(maxWidth: .infinity, alignment: .leading)
                    RunvealStat(
                        value: elevationValue,
                        label: RunvealDesignSystemStrings.courseCardStatElevation,
                        unit: elevationUnit,
                        type: .small
                    )
                    .frame(maxWidth: .infinity, alignment: .leading)
                    RunvealStat(
                        value: newRoadValue,
                        label: RunvealDesignSystemStrings.courseCardStatNewRoad,
                        unit: newRoadUnit,
                        type: .small,
                        accent: true
                    )
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .frame(maxWidth: .infinity)
            }
            .padding(.top, 16)
            .padding(.trailing, 12)
            .padding(.bottom, 20)
            .padding(.leading, 20)
            .frame(width: Self.width, alignment: .leading)
            .background(colors.surface.default, in: RunvealShapes.radius16)
            .overlay { RunvealShapes.radius16.stroke(borderColor, lineWidth: 1) }
            .contentShape(RunvealShapes.radius16)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    RunvealCourseCard(
        name: "성수동 한 바퀴",
        tags: ["공원", "순환"],
        distanceValue: "5.2",
        distanceUnit: "km",
        timeValue: "32",
        timeUnit: "분",
        elevationValue: "+45",
        elevationUnit: "m",
        newRoadValue: "1.3",
        newRoadUnit: "km",
        state: .selected
    ) {}
        .padding()
        .background(RunvealTheme.colors.bg.canvas)
}
