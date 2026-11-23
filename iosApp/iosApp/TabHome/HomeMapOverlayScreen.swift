import SharedLogic
import SwiftUI

enum TabHomeStrings {
    static let currentLocation = "현재 위치"
    static let headline = "오늘도 새로운 길을\n밝혀볼까요?"
    static let toggleExplored = "탐험한 길"
    static let toggleUnexplored = "안 가본 길"
    static let myNeighborhood = "내 동네"
    static let exploredSuffix = "탐험했어요"
    static let createCourse = "코스 만들기"
    static let myLocation = "내 위치"
    static func cumulative(_ exploredKm: Double, _ totalKm: Double) -> String {
        "누적 \(exploredKm)km\n전체 \(totalKm)km"
    }
}

/// 보기 전환 토글에서 "안 가본 길"의 인덱스 — 아래 옵션 목록 순서와 짝이다.
private let toggleIndexUnexplored = 1

/// 홈 지도 오버레이. 지도 자체와 위치 권한 요청은 Map Shell([RunvealHomeShell])이 맡고,
/// 여기서는 줌 레벨을 읽어 그 위에 얹는 UI만 그린다. Android `HomeMapOverlayScreen`에 해당한다.
struct HomeMapOverlayScreen: View {
    @ObservedObject var mapState: RunvealMapState
    var onCreateCourseClick: () -> Void = {}
    var onRunStartClick: () -> Void = {}

    @State private var selectedToggleIndex = 0

    /// 현재 위치의 행정동 — 서버에 한 번 물어 둔다(`IosCurrentDong`). 못 받으면 nil이고 화면은
    /// 대체 문구를 쓴다.
    @State private var currentDong: CurrentDong?

    /// 스플래시가 미리 만들어 둔 오버레이 — 탐험률·거리를 여기서 꺼낸다.
    @ObservedObject private var overlayHolder = RunvealDongOverlayHolder.shared

    var body: some View {
        Group {
            if mapState.isZoomedIn {
                // TODO: 동 상세 진입은 아직 — Android도 `onDongClick = {}`으로 비워 둔 상태다.
                ZoomedInMapOverlay(
                    dongName: currentDong?.name ?? TabHomeStrings.currentLocation,
                    ratePercent: ratePercent,
                    exploredKm: exploredKm,
                    totalKm: totalKm,
                    selectedToggleIndex: selectedToggleIndex,
                    onToggleSelect: { selectedToggleIndex = $0 },
                    onDongClick: {},
                    onCreateCourseClick: onCreateCourseClick,
                    onRunStartClick: onRunStartClick,
                    onMyLocationClick: { mapState.recenterToCurrentLocation() }
                )
            } else {
                HomeMapOverlay(locationName: currentDong?.name ?? TabHomeStrings.currentLocation)
            }
        }
        .task {
            // 위치를 못 받으면(권한 없음·위치 꺼짐) 동 조회를 건너뛴다 — 흔한 상황이라 오류가 아니다.
            guard let location = await RunvealCurrentLocation().fetch() else { return }
            // Kotlin suspend 함수는 Swift에 async throws로 들어온다(완료 핸들러가 NSError를
            // 받기 때문) — 실패하면 동 이름 없이 두면 되므로 try?로 흘린다.
            currentDong = try? await IosCurrentDong.shared.fetch(lat: location.lat, lon: location.lon)
        }
        // "안 가본 길"을 고른 동안만 베이스 지도 도로를 강조한다 — 줌 아웃되거나 이 화면을 벗어나면
        // 원래대로.
        .onChange(of: unexploredRoadFocus) { _, focus in
            mapState.unexploredRoadFocus = focus
        }
        .onAppear { mapState.unexploredRoadFocus = unexploredRoadFocus }
        .onDisappear { mapState.unexploredRoadFocus = false }
    }

    // 탐험률·거리는 지도 빛과 **같은 집계**에서 꺼낸다 — 다른 데서 계산하면 헤더는 8%인데 지도는
    // 안 빛나는 상태가 된다. 동 코드를 아직 못 받았거나 그 동이 집계에 없으면 0으로 둔다.
    private var ratePercent: Int {
        guard let code = currentDong?.code else { return 0 }
        // Kotlin `Int`는 Swift에 `Int32`로 넘어온다.
        return Int(overlayHolder.bundle?.query.percentOf(admCd2: code) ?? 0)
    }

    private var exploredKm: Double {
        guard let code = currentDong?.code else { return 0 }
        return (overlayHolder.bundle?.query.walkedMetersOf(admCd2: code) ?? 0) / 1000.0
    }

    private var totalKm: Double {
        guard let code = currentDong?.code else { return 0 }
        return (overlayHolder.bundle?.query.totalMetersOf(admCd2: code) ?? 0) / 1000.0
    }

    private var unexploredRoadFocus: Bool {
        mapState.isZoomedIn && selectedToggleIndex == toggleIndexUnexplored
    }
}

/// 줌아웃 상태 오버레이 — 상단 배경 + 빛 이미지 + 현재 위치 라벨 + 헤드라인.
private struct HomeMapOverlay: View {
    let locationName: String

    var body: some View {
        ZStack(alignment: .topLeading) {
            VStack(spacing: 0) {
                RunvealTheme.colors.bg.canvas
                    .frame(height: 200)
                Spacer()
            }
            .ignoresSafeArea(edges: .top)

            // 위아래로 서서히 사라지는 마스크를 씌운 빛 이미지 — Android는 `BlendMode.DstIn`으로
            // 같은 그라데이션을 적용한다.
            RunvealImages.homeLightImg
                .resizable()
                .scaledToFit()
                .mask(
                    LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0),
                            .init(color: .white, location: 0.5171),
                            .init(color: .white, location: 0.5211),
                            .init(color: .clear, location: 0.9471),
                            .init(color: .clear, location: 1),
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
                .padding(.top, 72)
                .padding(.trailing, 40)
                .frame(maxWidth: .infinity, alignment: .trailing)
                .allowsHitTesting(false)

            VStack(alignment: .leading, spacing: 0) {
                CurrentLocationLabel(locationName: locationName)
                Spacer().frame(height: 10)
                Text(TabHomeStrings.headline)
                    .runvealTextStyle(RunvealTheme.typography.heading1Semibold)
                    .foregroundStyle(RunvealTheme.colors.text.primary)
            }
            .padding(.leading, RunvealSpacing.space24)
            .padding(.top, 48)
            .padding(.trailing, RunvealLayout.marginScreen)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}

private struct CurrentLocationLabel: View {
    let locationName: String

    var body: some View {
        let colors = RunvealTheme.colors
        HStack(spacing: RunvealSpacing.space4) {
            RunvealIcon(RunvealIcons.pin, tint: colors.icon.primary)
            Text(locationName)
                .runvealTextStyle(RunvealTheme.typography.bodyLSemibold)
                .foregroundStyle(colors.text.primary)
        }
    }
}

/// A3-4(줌 인) 오버레이. 지도/탭바는 제외, 요약 카드 + 하단 액션 줄만 담당.
private struct ZoomedInMapOverlay: View {
    let dongName: String
    let ratePercent: Int
    let exploredKm: Double
    let totalKm: Double
    let selectedToggleIndex: Int
    let onToggleSelect: (Int) -> Void
    let onDongClick: () -> Void
    let onCreateCourseClick: () -> Void
    let onRunStartClick: () -> Void
    let onMyLocationClick: () -> Void

    @Environment(\.runvealMapOverlayBottomInset) private var overlayBottomInset

    var body: some View {
        ZStack(alignment: .top) {
            VStack(alignment: .leading, spacing: RunvealSpacing.space16) {
                ExploreSummaryCard(
                    dongName: dongName,
                    ratePercent: ratePercent,
                    exploredKm: exploredKm,
                    totalKm: totalKm,
                    onClick: onDongClick
                )
                RunvealSegmentedToggle(
                    options: [TabHomeStrings.toggleExplored, TabHomeStrings.toggleUnexplored],
                    selectedIndex: selectedToggleIndex,
                    onSelect: onToggleSelect
                )
                .padding(.leading, RunvealLayout.marginScreen)
                Spacer()
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            VStack {
                Spacer()
                MapActionRow(
                    onCreateCourseClick: onCreateCourseClick,
                    onRunStartClick: onRunStartClick,
                    onMyLocationClick: onMyLocationClick,
                    bottomInset: overlayBottomInset
                )
            }
        }
    }
}

/// 하단 액션 줄 - 코스 만들기 / 러닝 시작 / 내 위치.
private struct MapActionRow: View {
    let onCreateCourseClick: () -> Void
    let onRunStartClick: () -> Void
    let onMyLocationClick: () -> Void
    /// 위치 권한/서비스 배너가 탭바 위에 얹혀 더 높아진 경우 Map Shell이 알려주는 차이.
    let bottomInset: CGFloat

    var body: some View {
        HStack(alignment: .bottom) {
            MapActionButton(icon: RunvealIcons.course, label: TabHomeStrings.createCourse, onClick: onCreateCourseClick)
            Spacer()
            RunvealRunStartButton(onClick: onRunStartClick)
            Spacer()
            MapActionButton(icon: RunvealIcons.locate, label: TabHomeStrings.myLocation, onClick: onMyLocationClick)
        }
        .padding(.horizontal, RunvealLayout.marginMapControls)
        // 플로팅 탭바(sizeTabBar + 상하 space8) 위로 띄운다.
        .padding(.bottom, RunvealLayout.sizeTabBar + RunvealSpacing.space32 + bottomInset)
        .frame(maxWidth: .infinity)
    }
}

private struct MapActionButton: View {
    let icon: RunvealIconSpec
    let label: String
    let onClick: () -> Void

    var body: some View {
        VStack(spacing: RunvealSpacing.space8) {
            RunvealIconButton(icon: icon, accessibilityLabel: label, onClick: onClick)
            Text(label)
                .runvealTextStyle(RunvealTheme.typography.captionSRegular)
                .foregroundStyle(RunvealTheme.colors.text.secondary)
        }
    }
}

/// 상단 고정 헤더 카드. 아래 모서리만 둥글게(20 — 매칭되는 radius 토큰 없어 하드코딩) 상태 바에 붙는다.
private struct ExploreSummaryCard: View {
    let dongName: String
    let ratePercent: Int
    let exploredKm: Double
    let totalKm: Double
    let onClick: () -> Void

    var body: some View {
        let colors = RunvealTheme.colors
        let shape = UnevenRoundedRectangle(
            topLeadingRadius: 0,
            bottomLeadingRadius: 20,
            bottomTrailingRadius: 20,
            topTrailingRadius: 0
        )

        VStack(alignment: .leading, spacing: RunvealSpacing.space8) {
            Button(action: onClick) {
                HStack(spacing: RunvealSpacing.space4) {
                    Text(TabHomeStrings.myNeighborhood)
                        .runvealTextStyle(RunvealTheme.typography.bodyLRegular)
                        .foregroundStyle(colors.text.secondary)
                    Text(dongName)
                        .runvealTextStyle(RunvealTheme.typography.heading3)
                        .foregroundStyle(colors.text.primary)
                    Spacer(minLength: 0)
                }
                .frame(maxWidth: .infinity)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            HStack {
                HStack(alignment: .lastTextBaseline, spacing: 0) {
                    Text("\(ratePercent)")
                        .runvealTextStyle(RunvealTheme.typography.numL)
                        .foregroundStyle(colors.text.accent)
                    Text(RunvealDesignSystemStrings.percentUnit)
                        .runvealTextStyle(RunvealTheme.typography.unitL)
                        .foregroundStyle(colors.text.accent)
                    Spacer().frame(width: RunvealSpacing.space4)
                    Text(TabHomeStrings.exploredSuffix)
                        .runvealTextStyle(RunvealTheme.typography.bodyLRegular)
                        .foregroundStyle(colors.text.primary)
                }
                Spacer()
                Text(TabHomeStrings.cumulative(exploredKm, totalKm))
                    .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                    .foregroundStyle(colors.text.tertiary)
                    .multilineTextAlignment(.trailing)
            }
            .frame(maxWidth: .infinity)
        }
        .padding(.horizontal, RunvealLayout.marginScreen)
        .padding(.vertical, RunvealLayout.marginCardPadding)
        .frame(maxWidth: .infinity)
        .background(colors.surface.raised, in: shape)
    }
}
