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
            // 시안(List/Course Card)은 내용 묶음과 셰브론이 나란히 선 가로 구성이다 — 셰브론은
            // 제목 줄이 아니라 **카드 세로 가운데**에 온다(Android와 같은 구성).
            HStack(spacing: RunvealSpacing.space12) {
                VStack(alignment: .leading, spacing: RunvealSpacing.space12) {
                    HStack(spacing: RunvealSpacing.space8) {
                        Text(name)
                            .runvealTextStyle(RunvealTheme.typography.heading3)
                            .foregroundStyle(nameColor)
                            .lineLimit(1)
                        if !tags.isEmpty {
                            HStack(alignment: .bottom, spacing: RunvealSpacing.space4) {
                                ForEach(tags.prefix(2), id: \.self) { tag in
                                    RunvealTag(label: tag)
                                }
                            }
                            // 이름이 길면 태그를 밀어내는 대신 이름이 줄어들게 한다.
                            .fixedSize()
                        }
                        Spacer(minLength: 0)
                    }
                    .frame(maxWidth: .infinity)

                    stats
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                RunvealIcon(RunvealIcons.chevron, tint: colors.icon.tertiary, size: 16)
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

    /// 카드 안의 수치 네 개. 시안(Stats 기본/축소)대로 **각 수치는 제 내용만큼만 차지하고 남는
    /// 공간은 오른쪽에 둔다** — 칸을 균등하게 나누면 "28.7km"와 "+145m"처럼 길이가 다른 값이 제
    /// 칸 안에서 왼쪽으로 몰려 수치 사이가 들쭉날쭉해 보인다.
    ///
    /// 대신 균등 분할이 해 주던 일(네 칸이 넘치지 않게 하는 것)을 `ViewThatFits`가 한다. 제 폭대로
    /// 늘어선 쪽이 안 들어가면 **축소 쪽**을 고른다 — 간격을 12에서 8로 줄이고 값(숫자+단위)만 한
    /// 단계 작게 쓴다(라벨은 그대로). 시안의 "Stat 너비 합 + 36 > 178"이 이 조건으로, 178 같은
    /// 숫자를 박아 두는 대신 실제로 주어진 폭으로 판단한다 — 카드 폭이나 좌우 여백이 바뀌어도
    /// 조건이 따라온다.
    private var stats: some View {
        ViewThatFits(in: .horizontal) {
            statsRow(compact: false)
            statsRow(compact: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func statsRow(compact: Bool) -> some View {
        HStack(alignment: .top, spacing: compact ? RunvealSpacing.space8 : RunvealSpacing.space12) {
            RunvealStat(
                value: distanceValue,
                label: RunvealDesignSystemStrings.courseCardStatDistance,
                unit: distanceUnit,
                type: .small,
                compactValue: compact
            )
            RunvealStat(
                value: timeValue,
                label: RunvealDesignSystemStrings.courseCardStatTime,
                unit: timeUnit,
                type: .small,
                compactValue: compact
            )
            RunvealStat(
                value: elevationValue,
                label: RunvealDesignSystemStrings.courseCardStatElevation,
                unit: elevationUnit,
                type: .small,
                compactValue: compact
            )
            RunvealStat(
                value: newRoadValue,
                label: RunvealDesignSystemStrings.courseCardStatNewRoad,
                unit: newRoadUnit,
                type: .small,
                accent: true,
                compactValue: compact
            )
        }
    }
}

// 위: 기본. 아래: 수치가 길어 축소 쪽으로 넘어가는 카드 — 시안의 "Stats 축소" 예시 값이다.
#Preview {
    VStack(spacing: 16) {
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
        RunvealCourseCard(
            name: "코스 A",
            tags: ["공원", "하천길"],
            distanceValue: "28.7",
            distanceUnit: "km",
            timeValue: "186",
            timeUnit: "분",
            elevationValue: "+145",
            elevationUnit: "m",
            newRoadValue: "25.0",
            newRoadUnit: "km"
        ) {}
    }
    .padding()
    .background(RunvealTheme.colors.bg.canvas)
}
