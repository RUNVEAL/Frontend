import SharedLogic
import SwiftUI

/// 홈 화면 — Map Shell. 지도([RunvealMap])는 여기서 하나만 그리고, 탭 화면과 지도 위 흐름(홈 지도,
/// 코스 추천, 러닝 기록/결과)은 전부 이 아래에서 조립한다. 탭바 노출 규칙도 여기 한 곳에만 둔다.
/// Android `HomeScreen`에 해당한다.
struct RunvealHomeShell: View {
    /// 홈 하단 패널이 탭바를 바깥에서 6만큼 감싼다(너비 = 탭바 + 좌우 6).
    private static let panelInnerInset: CGFloat = 6

    /// 토스트가 떠 있는 시간 — 디자인시스템 토스트 주석의 "3초"를 그대로 따른다.
    private static let toastDuration: Duration = .seconds(3)

    /// 줌인 오버레이의 하단 액션 줄이 위치 안내 배너 때문에 더 높아진 패널과 안 겹치게 추가로 띄워야
    /// 하는 높이. 배너(40) + 패널 안쪽 top padding(12) + 배너·탭바 사이 gap(4) - 패널이 대신하는
    /// 탭바 단독 바깥 padding(16) = 56. 매칭되는 토큰이 없어 직접 계산해 하드코딩했다(Android와 같다).
    private static let locationBannerExtraInset: CGFloat = 56

    @StateObject private var navigator = RunvealHomeNavigator()
    @StateObject private var mapState = RunvealMapState()
    @StateObject private var mapData = HomeMapDataModel()
    @StateObject private var permissionState = RunvealLocationPermissionState()
    @ObservedObject private var pendingCourseRun = RunvealPendingCourseRun.shared

    @State private var toastMessage: String?

    /**
     러닝을 시작하기 전에 위치가 준비됐는지 보고, 안 됐으면 그 자리에서 안내 시트를 띄운다.
     여기에 담긴 동작은 권한·위치 서비스가 갖춰지면 이어서 실행된다.

     그냥 보내면 기록이 시작돼도 좌표가 하나도 안 들어와서, 러닝이 끝날 때까지 빈 화면을 보게 된다
     (Android는 Foreground Service가 SecurityException으로 죽는 자리다 — 증상은 달라도 원인은 같다).

     **코스 탐색에는 걸지 않는다.** 코스를 찾아보는 데까지는 위치가 없어도 되고, 실제로 뛰기
     시작할 때만 필요하다.
     */
    @State private var pendingRunStart: (() -> Void)?

    /// 위치가 준비됐으면 바로 실행하고, 아니면 시트를 띄운 뒤 갖춰질 때까지 미뤄 둔다.
    private func requireLocationForRun(_ proceed: @escaping () -> Void) {
        if permissionState.isGranted && permissionState.isLocationServicesEnabled {
            proceed()
        } else {
            pendingRunStart = proceed
        }
    }

    /// 위치가 준비된 뒤 한 번만 현재 위치로 옮겼는가 — 앱을 켠 뒤 처음 한 번이다.
    @State private var didRecenterOnLocationReady = false

    /**
     위치 권한과 위치 서비스가 모두 갖춰지면 지도를 **기본 위치(서울시청)에서 현재 위치로** 한 번
     옮긴다.

     지도는 권한을 받기 **전에** 떠 있다 — 권한 안내 시트가 지도 위에 뜨기 때문이다. 그래서 허용한
     뒤에 옮겨 주는 경로가 없으면 앱을 다시 켤 때까지 기본 위치에 머문다. 이미 허용된 상태로 켰을
     때도 첫 등장에서 한 번 옮긴다(Android `CurrentLocationLayer.activate`가 켜지면서 `trackOnce`로
     하는 일과 같다).

     계속 따라가지는 않는다 — 한 번 옮기고 나면 사용자가 자유롭게 둘러볼 수 있어야 한다.
     */
    private func recenterOnceWhenLocationReady() {
        guard !didRecenterOnLocationReady else { return }
        guard permissionState.isGranted, permissionState.isLocationServicesEnabled else { return }
        didRecenterOnLocationReady = true
        mapState.recenterToCurrentLocation()
    }

    /// 시트에서 허용·켜기를 마치면 다시 누르게 하지 않고 바로 잇는다.
    private func resumePendingRunIfReady() {
        guard let proceed = pendingRunStart else { return }
        guard permissionState.isGranted && permissionState.isLocationServicesEnabled else { return }
        pendingRunStart = nil
        proceed()
    }

    private var isLocationBannerNeeded: Bool {
        !permissionState.isGranted || !permissionState.isLocationServicesEnabled
    }

    /// 지도를 깔고 그 위에 올라가는 코스 흐름 — 추천(만들기·결과·상세)과 "내 코스"에서 들어가는
    /// 코스 상세. 어느 쪽이든 동 경계·라벨을 걷어 코스에만 집중하게 한다(Android와 같은 규칙).
    private var isCourseRecommendFlow: Bool {
        switch navigator.currentOverlay {
        case .courseRecommend, .courseRecommendResult, .courseDetail, .savedCourseDetail:
            return true
        default:
            return false
        }
    }

    /// 지도 위 흐름(코스 추천~상세, 러닝 기록, 러닝 결과)에 들어가 있을 때만 탭바를 완전히 숨긴다.
    private var showTabBar: Bool { !navigator.isOverlayFlowActive }

    private var tabItems: [RunvealTabItemSpec] {
        [
            RunvealTabItemSpec(icon: RunvealIcons.home, label: HomeStrings.tabHome),
            RunvealTabItemSpec(icon: RunvealIcons.course, label: HomeStrings.tabCourse),
            RunvealTabItemSpec(icon: RunvealIcons.record, label: HomeStrings.tabRecord),
            RunvealTabItemSpec(icon: RunvealIcons.my, label: HomeStrings.tabMy),
        ]
    }

    var body: some View {
        ZStack {
            RunvealMap(
                state: mapState,
                overlay: mapData.visibleOverlay(hideRegionOverlays: isCourseRecommendFlow)
            )
            .ignoresSafeArea()
            .runvealMapLayers(mapState, mapData.layers)

            content

            if let toastMessage {
                VStack {
                    RunvealToast(message: toastMessage)
                        .padding(.horizontal, RunvealLayout.marginScreen)
                        .padding(.vertical, RunvealSpacing.space16)
                    Spacer()
                }
            }

            if showTabBar {
                VStack {
                    Spacer()
                    bottomBar
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .environment(\.runvealMapOverlayBottomInset, isLocationBannerNeeded ? Self.locationBannerExtraInset : 0)
        .requireLocationPermission(permissionState)
        .onAppear {
            mapData.start()
            recenterOnceWhenLocationReady()
        }
        .onDisappear { mapData.stop() }
        // "내 코스" 탭에서 코스를 바로 시작하면(탭 화면은 내비게이터를 직접 못 만진다) 여기서 받아
        // 기록 흐름으로 넘어간다 — 코스 자체는 `SelectedCourseHolder`에 이미 담겨 있다.
        .onChange(of: pendingCourseRun.requestId) { _, newValue in
            guard newValue > 0 else { return }
            navigator.replaceStack(with: .recording)
        }
        // 토스트 자체는 타이머를 모르는 순수 컴포넌트라 사라지는 건 여기서 재운다.
        .task(id: toastMessage) {
            guard toastMessage != nil else { return }
            try? await Task.sleep(for: Self.toastDuration)
            toastMessage = nil
        }
        // 러닝 시작을 눌렀는데 위치가 준비 안 된 경우 — 무엇이 모자란지에 맞는 시트를 띄운다.
        // 권한을 먼저 보고, 권한이 있으면 위치 서비스를 본다(둘 중 하나만 꺼져도 좌표를 못 받는다).
        .sheet(isPresented: Binding(get: { pendingRunStart != nil },
                                    set: { if !$0 { pendingRunStart = nil } })) {
            runStartLocationSheet
                .runvealSheetContent()
        }
        // 시트에서 허용·켜기를 마치고 돌아오면 미뤄 둔 러닝 시작을 잇는다.
        .onChange(of: permissionState.isGranted) { _, _ in
            recenterOnceWhenLocationReady()
            resumePendingRunIfReady()
        }
        .onChange(of: permissionState.isLocationServicesEnabled) { _, _ in
            recenterOnceWhenLocationReady()
            resumePendingRunIfReady()
        }
    }

    // MARK: - 탭/오버레이 콘텐츠

    @ViewBuilder
    private var content: some View {
        if let overlay = navigator.currentOverlay {
            overlayContent(overlay)
        } else {
            tabContent
        }
    }

    @ViewBuilder
    private var tabContent: some View {
        switch navigator.selectedTab {
        case .home:
            HomeMapOverlayScreen(
                mapState: mapState,
                // 코스 탐색은 위치 없이도 들어갈 수 있다 — 실제로 뛸 때만 위치가 필요하다.
                onCreateCourseClick: { navigator.push(.courseRecommend) },
                onRunStartClick: { requireLocationForRun { navigator.push(.recording) } }
            )
        case .course:
            CourseScreen(
                onCreateCourseClick: { navigator.push(.courseRecommend) },
                onCourseClick: { course in navigator.push(.savedCourseDetail(courseId: course.id)) }
            )
        case .record:
            RecordScreen(
                onRecordClick: { trackId in navigator.push(.recordDetail(trackId: trackId)) },
                // 기록이 없을 때의 "러닝 시작하기" — 홈 탭으로 옮기고 지도를 줌인 뷰까지 확대한다.
                // 줌인이라야 러닝 시작 버튼이 있는 오버레이가 뜬다(Android와 같은 흐름).
                onStartRunningClick: {
                    navigator.select(tab: .home)
                    mapState.recenterToCurrentLocation(zoom: RunvealMapConfig.homeZoomIn)
                }
            )
        case .my:
            MyScreen(
                permissionState: permissionState,
                onTermsItemClick: { item in navigator.push(.termsDetail(item: item)) }
            )
        }
    }

    @ViewBuilder
    private func overlayContent(_ overlay: RunvealHomeOverlayRoute) -> some View {
        switch overlay {
        case .courseRecommend:
            CourseCreateScreen(
                mapState: mapState,
                onBack: { navigator.pop() },
                onNavigateToResult: { navigator.push(.courseRecommendResult) }
            )
        case .courseRecommendResult:
            CourseRecommendResultScreen(
                mapState: mapState,
                onClose: { navigator.pop() },
                onCourseClick: { routeId in navigator.push(.courseDetail(routeId: routeId)) }
            )
        case .courseDetail(let routeId):
            CourseDetailScreen(
                mapState: mapState,
                source: .recommended(id: routeId),
                onBack: { navigator.pop() },
                // 코스 추천 흐름 스택을 전부 지우고 들어간다 — 안 지우면 러닝 종료 후 뒤로가기/닫기로
                // 홈이 아니라 코스 추천 화면으로 돌아간다.
                // 코스 상세에서 "이 코스로 러닝 시작" — 여기서부터 위치가 필요하다.
                onStartRunning: { requireLocationForRun { navigator.replaceStack(with: .recording) } }
            )
        case .savedCourseDetail(let courseId):
            CourseDetailScreen(
                mapState: mapState,
                source: .saved(id: courseId),
                onBack: { navigator.pop() },
                // 상세를 먼저 떼어낸다 — 러닝이 끝나고 돌아올 자리는 "내 코스" 목록이다.
                onStartRunning: {
                    requireLocationForRun {
                        navigator.pop()
                        navigator.push(.recording)
                    }
                }
            )
        case .recording:
            RecordingScreen(
                mapState: mapState,
                // 기록 화면을 스택에서 빼야 결과에서 닫으면 홈이 된다.
                onFinished: { navigator.replaceStack(with: .result) },
                // 저장하지 않은 러닝은 결과 화면이 없으니 홈 지도로 되돌리고, 왜 기록이 안 남았는지
                // 토스트로 알려 준다.
                onDiscarded: {
                    navigator.popToTab()
                    toastMessage = HomeStrings.recordingDiscardedToast
                }
            )
        case .result:
            ResultScreen(onClose: { navigator.popToTab() })
        case .recordDetail(let trackId):
            RecordDetailScreen(trackId: trackId, onBack: { navigator.pop() })
        case .termsDetail(let item):
            TermsDetailScreen(item: item, onBack: { navigator.pop() })
        }
    }

    // MARK: - 하단 패널/탭바

    @ViewBuilder
    private var bottomBar: some View {
        let banner = locationOffBanner
        let isZoomedIn = mapState.isZoomedIn

        if navigator.selectedTab == .home && !isZoomedIn {
            // 홈 탭 · 줌아웃: 패널 전체(배너? + 요약 카드 + 탭바).
            RunvealHomeBottomPanel(
                banner: banner,
                // TODO: 러닝 기록 연동 추가 예정(Android도 같은 TODO 상태다).
                summary: AnyView(
                    RunvealSummaryCard(dongName: "내 동네", ratePercent: 0, isMine: true) {}
                ),
                onMyLocationClick: { mapState.recenterToCurrentLocation() }
            ) {
                tabBar(container: .embedded)
            }
            .padding(.horizontal, RunvealLayout.marginScreen)
            .padding(.top, RunvealSpacing.space8)
            .padding(.bottom, RunvealSpacing.space8 - Self.panelInnerInset)
        } else if navigator.selectedTab == .home && isZoomedIn && banner != nil {
            // 홈 탭 · 줌인 + 위치 꺼짐: 요약 카드는 생략하고 배너만 탭바 위에 얹는다.
            RunvealHomeBottomPanel(banner: banner) {
                tabBar(container: .embedded)
            }
            .padding(.horizontal, RunvealLayout.marginScreen)
            .padding(.top, RunvealSpacing.space8)
            .padding(.bottom, RunvealSpacing.space8 - Self.panelInnerInset)
        } else {
            // 그 외(줌인 + 위치 정상, 다른 탭): 탭바만 홀로 띄운다.
            tabBar(container: .floating)
                .padding(.horizontal, RunvealLayout.marginScreen + Self.panelInnerInset)
                .padding(.vertical, RunvealSpacing.space8)
        }
    }

    private func tabBar(container: RunvealTabBarContainer) -> some View {
        RunvealTabBar(
            items: tabItems,
            selectedIndex: navigator.selectedTab.rawValue,
            container: container
        ) { index in
            guard let tab = RunvealHomeTab(rawValue: index) else { return }
            navigator.select(tab: tab)
        }
    }

    @ViewBuilder
    private var runStartLocationSheet: some View {
        if permissionState.isPermanentlyDenied {
            RunvealLocationPermissionSettingsSheet(
                onSettingsClick: { permissionState.openSettings() },
                onCloseClick: { pendingRunStart = nil }
            )
        } else if !permissionState.isGranted {
            RunvealLocationPermissionSheet(
                onAllowClick: { permissionState.request() },
                onLaterClick: { pendingRunStart = nil }
            )
        } else {
            RunvealLocationServicesSheet(
                onSettingsClick: { permissionState.requestEnableLocationServices() },
                onCloseClick: { pendingRunStart = nil }
            )
        }
    }

    private var locationOffBanner: AnyView? {
        if !permissionState.isGranted {
            return AnyView(
                RunvealLocationOffBanner(
                    message: HomeStrings.locationPermissionOffBannerMessage,
                    actionLabel: HomeStrings.locationPermissionOffBannerAction
                ) {
                    // 거부가 굳어지면 시스템이 권한 다이얼로그를 더 띄워 주지 않는다 — 그대로
                    // request()를 부르면 아무 일도 안 일어난 것처럼 보인다. 그때는 설정으로 보낸다
                    // (마이페이지의 위치 권한 줄과 같은 처리).
                    if permissionState.isPermanentlyDenied {
                        permissionState.openSettings()
                    } else {
                        permissionState.request()
                    }
                }
            )
        }
        if !permissionState.isLocationServicesEnabled {
            return AnyView(
                RunvealLocationOffBanner(
                    message: HomeStrings.locationServicesOffBannerMessage,
                    actionLabel: HomeStrings.locationServicesOffBannerAction
                ) {
                    permissionState.requestEnableLocationServices()
                }
            )
        }
        return nil
    }
}
