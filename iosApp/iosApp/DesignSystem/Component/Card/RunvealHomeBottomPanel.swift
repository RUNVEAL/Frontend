import SwiftUI

/// 홈 하단 패널. GNB 위치는 다른 탭과 같게 고정, 패널이 바깥에서 감싼다(너비 = GNB + 좌우 6).
/// 보통 [summary]엔 [RunvealSummaryCard], [tabBar]엔
/// `RunvealTabBar(container: .embedded)`를 넣는다.
/// 위치 권한/서비스가 꺼져 있으면 [banner]에 [RunvealLocationOffBanner]를 넣는다 —
/// [summary]를 대체하지 않고 그 위에 구분선과 함께 얹힌다. 지도를 줌인했을 때처럼 [summary]를
/// 보여줄 자리가 없을 땐 nil로 생략할 수 있다(이때도 [banner]는 [tabBar] 위에 그대로 얹힌다).
/// [onMyLocationClick]을 주면 패널 우측 상단 바로 위에 "내 위치" 아이콘 버튼이 떠 있는 형태로 추가된다.
///
/// 슬롯을 `AnyView?`로 받는다 — Android의 `(@Composable () -> Unit)?`와 같은 "있을 수도 없을 수도
/// 있는 슬롯"을 제네릭으로 표현하면 호출부마다 타입이 갈려서, 배너 유무로 구분선을 넣는 판단을
/// 할 수 없다.
struct RunvealHomeBottomPanel: View {
    private static let innerInset: CGFloat = 6

    private let tabBar: AnyView
    private let banner: AnyView?
    private let summary: AnyView?
    private let onMyLocationClick: (() -> Void)?

    init<TabBar: View>(
        banner: AnyView? = nil,
        summary: AnyView? = nil,
        onMyLocationClick: (() -> Void)? = nil,
        @ViewBuilder tabBar: () -> TabBar
    ) {
        self.tabBar = AnyView(tabBar())
        self.banner = banner
        self.summary = summary
        self.onMyLocationClick = onMyLocationClick
    }

    var body: some View {
        let colors = RunvealTheme.colors
        ZStack(alignment: .topTrailing) {
            VStack(spacing: RunvealSpacing.space4) {
                if let banner {
                    banner
                    // summary가 없으면(줌인 상태) 배너가 탭바 바로 위에 구분선 없이 얹힌다.
                    if summary != nil {
                        Rectangle()
                            .fill(colors.border.default)
                            .frame(height: 1)
                            .padding(.horizontal, RunvealSpacing.space20)
                    }
                }
                if let summary { summary }
                tabBar
            }
            .padding(.top, RunvealSpacing.space12)
            .padding(.leading, Self.innerInset)
            .padding(.trailing, Self.innerInset)
            .padding(.bottom, Self.innerInset)
            .frame(maxWidth: .infinity)
            .runvealGlass(RunvealShapes.radius32)
            .clipShape(RunvealShapes.radius32)

            if let onMyLocationClick {
                RunvealIconButton(
                    icon: RunvealIcons.locate,
                    accessibilityLabel: RunvealDesignSystemStrings.homeBottomPanelMyLocationContentDescription,
                    onClick: onMyLocationClick
                )
                .padding(.trailing, RunvealSpacing.space4)
                .offset(y: -(RunvealLayout.sizeIconButtonL + RunvealSpacing.space12))
            }
        }
    }
}

/// 위치 권한/위치 서비스가 꺼져 있을 때 [RunvealHomeBottomPanel]의 banner 자리에 넣는 한 줄짜리
/// 안내 배너. 권한이 꺼진 경우와 위치 서비스(GPS)가 꺼진 경우는 문구·행동이 달라 호출하는 쪽에서
/// [message]/[actionLabel]로 구분해 넣는다.
struct RunvealLocationOffBanner: View {
    private let message: String
    private let actionLabel: String
    private let onActionClick: () -> Void

    init(message: String, actionLabel: String, onActionClick: @escaping () -> Void) {
        self.message = message
        self.actionLabel = actionLabel
        self.onActionClick = onActionClick
    }

    var body: some View {
        let colors = RunvealTheme.colors
        HStack(spacing: RunvealSpacing.space8) {
            // tint를 주면 location-off 아이콘의 사선이 두꺼워 보인다 — 벡터 자체 색
            // (전경 F4EEEA + 배경 272626 knockout)을 그대로 쓴다.
            RunvealIcon(RunvealIcons.locationOff, size: 16)
            Text(message)
                .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                .foregroundStyle(colors.text.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button(action: onActionClick) {
                Text(actionLabel)
                    .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                    .foregroundStyle(colors.text.accent)
                    .padding(.horizontal, RunvealSpacing.space12)
                    .frame(height: 40)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
        .padding(.leading, RunvealSpacing.space20)
        .padding(.trailing, RunvealSpacing.space12)
        .frame(maxWidth: .infinity)
    }
}

/// 프리뷰용 탭 4개 — 실제 탭 목록은 앱 셸(RunvealHomeShell)이 들고 있다.
private var previewTabItems: [RunvealTabItemSpec] {
    [
        RunvealTabItemSpec(icon: RunvealIcons.home, label: "홈"),
        RunvealTabItemSpec(icon: RunvealIcons.course, label: "내 코스"),
        RunvealTabItemSpec(icon: RunvealIcons.record, label: "기록"),
        RunvealTabItemSpec(icon: RunvealIcons.my, label: "MY"),
    ]
}

#Preview {
    VStack(spacing: 24) {
        RunvealHomeBottomPanel(
            summary: AnyView(RunvealSummaryCard(dongName: "성수동", ratePercent: 37, isMine: true) {})
        ) {
            RunvealTabBar(items: previewTabItems, selectedIndex: 0, container: .embedded) { _ in }
        }

        RunvealHomeBottomPanel(
            banner: AnyView(
                RunvealLocationOffBanner(message: "위치 서비스가 꺼져 있어요", actionLabel: "위치 켜기") {}
            )
        ) {
            RunvealTabBar(items: previewTabItems, selectedIndex: 0, container: .embedded) { _ in }
        }
    }
    .padding()
    .background(RunvealTheme.colors.bg.canvas)
}
