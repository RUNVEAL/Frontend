import SharedLogic
import SwiftUI

/// B1 — `/api/v1/routes` 응답을 지도 위 오버레이로 보여주는 결과 화면. 지도(Map Shell)는 그대로 보이고
/// 이 위에 앱바와 하단 코스 카드 리스트만 얹는다 — 배경을 깔지 않는다.
/// 단 **추천 결과가 없을 때만** 한 장짜리 안내 화면이라 배경을 덮는다(시안 B1-1).
/// Android `CourseRecommendResultScreen`에 해당한다.
struct CourseRecommendResultScreen: View {
    @ObservedObject var mapState: RunvealMapState
    let onClose: () -> Void
    let onCourseClick: (String) -> Void

    @StateObject private var viewModel = CourseRecommendResultViewModel()
    @State private var dotMarkersOwner = UUID()
    /// 캐러셀 가운데에 와 있는 카드 — 선택 상태는 이 값이 정한다([courseCarousel]).
    @State private var centeredRouteId: String?
    /**
     경로를 지도 위 UI **안쪽**에 맞추려고 재 두는 자리 — 셋 다 **화면 절대 좌표(`.global`)**다.

     **크기(`geometry.size`)를 쓰면 안 된다.** 지도는 엣지투엣지(`RunvealMap().ignoresSafeArea()`)라
     카메라 여백(`setVisibleCoordinateBounds(edgePadding:)`)이 **화면 가장자리** 기준인데, 이 화면의
     뷰들은 안전 영역 **안쪽**에 놓인다. 그래서 뷰 크기를 그대로 넘기면 위는 상태바, 아래는 홈
     인디케이터 높이만큼 덜 비켜서 경로가 칩 줄·카드 쪽으로 밀려 붙는다(실기에서 그렇게 보였다).

     안전 영역 높이를 상수로 빼는 대신 **지도와 같은 원점(window)** 에서 잰 절대 좌표를 쓴다 —
     그러면 인셋이 얼마인지 이 코드가 알 필요가 없다(Android도 `boundsInWindow()`로 같은 방식이다).

     처음엔 아직 못 재서 0이고, 그동안은 카메라를 맞추지 않는다([focusSelected]).
     */
    @State private var topChromeBottom: CGFloat = 0
    @State private var cardsTop: CGFloat = 0
    @State private var mapViewportHeight: CGFloat = 0

    /// `RunvealCourseCard`의 고정 너비(240)와 맞춰야 캐러셀 가운데 정렬 계산이 들어맞는다.
    private static let cardWidth: CGFloat = 240

    /// 결과 없음 안내가 시작하는 자리 — 앱바 아래 24(시안 B1-1의 Contents y=188).
    /// 결과가 없을 땐 조건 칩 행을 숨기므로 앱바 높이만 비키면 된다.
    private static let emptyResultTopInset: CGFloat = RunvealLayout.sizeTopAppBar + RunvealSpacing.space24

    private var ready: CourseRecommendResultReady? {
        guard case .ready(let ready) = viewModel.state else { return nil }
        return ready
    }

    /// 결과가 없을 때는 지도 위 오버레이가 아니라 **한 장짜리 안내 화면**이다(시안 B1-1).
    private var isEmptyResult: Bool { ready?.emptyMessage != nil }

    var body: some View {
        ZStack(alignment: .top) {
            // 뒤에 지도가 비치면 "코스를 못 찾았다"면서 지도만 덩그러니 보이게 되고, 이전에 그려 둔
            // 경로선까지 안내 카드 위로 비친다 — 화면 배경색으로 덮는다(Android도 같다).
            if isEmptyResult {
                RunvealTheme.colors.bg.canvas.ignoresSafeArea()
            }

            topBar
                .background(GlobalBoundsReader { topChromeBottom = $0.maxY })

            switch viewModel.state {
            case .loading:
                ProgressView()
                    .tint(RunvealTheme.colors.accent.default)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            case .ready(let ready):
                if let message = ready.emptyMessage {
                    // 결과가 없을 때는 캐러셀과 달리 **화면 전체**를 쓴다 — 안내와 조건 변경 줄은
                    // 앱바 아래에서 시작하고, 버튼은 아래에 붙는다(시안 B1-1, Android와 같은 배치).
                    EmptyResult(
                        message: message,
                        settings: ready.settings,
                        onIntent: viewModel.onIntent,
                        onRetry: onClose
                    )
                    .padding(.horizontal, RunvealLayout.marginScreen)
                    .padding(.top, Self.emptyResultTopInset)
                    .padding(.bottom, RunvealSpacing.space16)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                } else {
                    VStack {
                        Spacer()
                        courseCarousel(ready: ready)
                            .background(GlobalBoundsReader { cardsTop = $0.minY })
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        // 조건을 바꿔 다시 받는 동안 — 조건 칩과 지도는 그대로 두고 로딩만 덮는다(화면이 비면
        // 뒤로 간 것처럼 보인다). Android도 같은 방식이다.
        .overlay {
            if ready?.isReloading == true {
                ProgressView()
                    .tint(RunvealTheme.colors.accent.default)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(RunvealTheme.colors.bg.canvas.opacity(0.6))
            }
        }
        // 지도 위에 얹는다 — `.sheet`으로 띄우면 뒤 지도가 검게 보인다(`runvealBottomSheet` 참고).
        .runvealBottomSheet(
            isPresented: ready?.filterDraft != nil,
            onDismiss: { viewModel.onIntent(.closeFilterSheet) }
        ) {
            if let draft = ready?.filterDraft {
                filterSheet(draft: draft)
            }
        }
        // 지도 뷰포트 높이 — 카메라 여백이 **화면 가장자리** 기준이라 안전 영역을 벗어난 전체
        // 높이가 필요하다. 그래서 이 판만 `ignoresSafeArea`로 화면 끝까지 늘려 잰다.
        .background(GlobalBoundsReader { mapViewportHeight = $0.maxY }.ignoresSafeArea())
        .runvealMapLayers(mapState, routeLayers)
        .runvealMapDotMarkers(mapState, courseMarkers, owner: dotMarkersOwner)
        // 선택된 코스가 바뀌면 그 코스가 전부 보이게 카메라를 맞춘다.
        .onChange(of: ready?.selectedRouteId) { _, _ in focusSelected() }
        .onChange(of: measuredChrome) { _, _ in focusSelected() }
        .onAppear { focusSelected() }
    }

    /// 코스 조건 설정 시트 — 코스 만들기에서 쓰는 것과 **같은 컴포넌트**다. 고친 값은 초안
    /// ([CourseRecommendResultReady.filterDraft])에만 쌓였다가 "코스 조건 적용하기"를 눌러야
    /// 서버 요청으로 나간다.
    private func filterSheet(draft: CourseRecommendSettings) -> some View {
        func update(_ next: CourseRecommendSettings) {
            viewModel.onIntent(.updateFilterDraft(next))
        }
        return FilterBottomSheet(
            selectedDistance: draft.selectedDistance,
            isCustomDistanceSelected: draft.isCustomDistanceSelected,
            customDistanceIndex: draft.customDistanceIndex,
            avoidVisitedPreferred: draft.avoidVisitedPreferred,
            selectedFilters: draft.selectedFilters,
            onDistanceSelect: { update(draft.with(selectedDistance: $0, isCustomDistanceSelected: false)) },
            onCustomDistanceChipClick: { update(draft.with(isCustomDistanceSelected: true)) },
            onCustomDistanceIndexChange: { update(draft.with(customDistanceIndex: $0)) },
            onAvoidVisitedPreferredChange: { update(draft.with(avoidVisitedPreferred: $0)) },
            onFilterToggle: { filter in
                var filters = draft.selectedFilters
                if filters.contains(filter) { filters.remove(filter) } else { filters.insert(filter) }
                update(draft.with(selectedFilters: filters))
            },
            onBack: { viewModel.onIntent(.closeFilterSheet) },
            onReset: { viewModel.onIntent(.resetFilterDraft) },
            onApply: { viewModel.onIntent(.applyFilterDraft) }
        )
    }

    // MARK: - 지도

    /// 코스가 바뀌거나(새 추천) 카드 선택이 바뀌면 지도에 그릴 선을 다시 만든다.
    private var routeLayers: [String: String] {
        guard let ready, !ready.routes.isEmpty else { return [:] }
        let routes = ready.routes.map { route in
            IosRecommendedRoute(
                id: route.routeId,
                points: route.path,
                selected: route.routeId == ready.selectedRouteId
            )
        }
        return [
            RunvealMapConfig.sourceRecommendedRoute: IosRouteGeoJson.shared.recommendedRoutes(routes: routes)
        ]
    }

    /// 선택된 코스의 출발/도착을 Dot 마커로 띄운다. 순환 코스는 도착이 곧 출발이라 — 같은 자리에
    /// 마커 2개가 겹쳐 보이지 않게 출발만 띄운다.
    private var courseMarkers: [MapPin] {
        guard let selected = ready?.selectedRoute else { return [] }
        var markers: [MapPin] = []
        if let start = selected.path.first {
            markers.append(MapPin(location: start, type: .start))
        }
        if !selected.isLoop, let end = selected.path.last {
            markers.append(MapPin(location: end, type: .end))
        }
        return markers
    }

    private func focusSelected() {
        guard let selected = ready?.selectedRoute, selected.path.count >= 2 else { return }
        // 아직 못 잰 동안(첫 프레임)은 맞추지 않는다 — 0으로 계산하면 엉뚱한 줌이 한 번 잡혔다가
        // 튄다. 측정값이 들어오면 [measuredChrome]을 보고 다시 부른다.
        guard topChromeBottom > 0, cardsTop > 0, mapViewportHeight > 0 else { return }

        // 시안(B1) 기준 — 경로는 조건 칩 줄 아래 16, 코스 카드 위 16, 좌우 16 안쪽에 들어와야 한다.
        let margin = RunvealLayout.marginScreen

        // 잰 값은 **컨테이너** 좌표라 그 안의 투명한 패딩까지 들어 있다. 기준은 "보이는 끝"이므로
        // 패딩만큼 되돌린다 — 안 그러면 위아래로 그 두께만큼 더 벌어진다.
        //
        // 여기서 칩 줄이 없는 경우는 없다 — 고를 코스가 있어야 이 함수가 돌고, 코스가 있으면
        // 칩 줄도 떠 있다(`emptyMessage == nil`).
        let chipsVisibleBottom = topChromeBottom - RunvealSpacing.space12
        let cardsVisibleTop = cardsTop + RunvealSpacing.space16

        mapState.focusOnBounds(
            points: selected.path,
            horizontalPadding: margin,
            // 절대 좌표라 상태바 높이가 이미 들어 있다 — 따로 더하지 않는다.
            topPadding: chipsVisibleBottom + margin,
            bottomPadding: mapViewportHeight - cardsVisibleTop + margin
        )
    }

    /// 측정값 묶음 — 하나라도 바뀌면 카메라를 다시 맞춘다(첫 측정이 들어오는 순간이 여기다).
    private var measuredChrome: [CGFloat] { [topChromeBottom, cardsTop, mapViewportHeight] }

    // MARK: - 조각

    private var topBar: some View {
        VStack(spacing: 0) {
            RunvealTopAppBar(
                leading: .back,
                // 지도 위에 얹힐 때만 글래스 원형 배경을 쓴다 — 결과가 없을 땐 불투명한 한 장짜리
                // 화면이라 아이콘만 둔다(시안 B1-1).
                style: isEmptyResult ? .default : .map,
                title: CourseRecommendStrings.resultTitle,
                onLeadingClick: onClose
            )
            // 결과가 없을 땐 칩을 숨긴다(시안 B1-1) — 보여 줄 코스가 없는데 조건만 떠 있으면 설명과
            // "조건 다시 설정하기" 버튼이 하는 말과 겹친다.
            if let ready, ready.emptyMessage == nil {
                SettingsSummaryRow(
                    settings: ready.settings,
                    onTap: { viewModel.onIntent(.openFilterSheet) }
                )
                .padding(.horizontal, RunvealLayout.marginScreen)
                .padding(.vertical, RunvealSpacing.space12)
            }
        }
        .frame(maxWidth: .infinity)
        // 머리 영역만 한 톤 올린다 — 상태 바까지 올라가야 경계가 생기지 않는다.
        .background {
            if isEmptyResult {
                RunvealTheme.colors.surface.raised.ignoresSafeArea(edges: .top)
            }
        }
    }

    /// 좌우 여백은 화면 마진(16)이고, 스냅이 **가장 가까운 카드를 가운데로** 보낸다([CenteredCardSnap]).
    ///
    /// 그래서 가운데 카드는 좌우에 이웃이 걸쳐 보이고, **첫 카드는 좌측 16·마지막 카드는 우측 16에
    /// 붙는다** — 양 끝에서는 더 스크롤할 데가 없어 가운데로 못 가고 잘리기 때문이다(시안).
    ///
    /// **가운데에 와 있는 카드가 선택이다** — Android(snapFling + 센터 감지)와 같은 규칙이다.
    /// `scrollTargetLayout()` + `scrollPosition(id:anchor: .center)`가 지금 가운데인 카드의 id를
    /// 알려 주므로, 쓸어 넘기다 멈추면 탭하지 않아도 그 카드가 금색 테두리가 된다.
    ///
    /// 탭은 선택을 바꾸지 않는다 — 가운데 카드를 탭하면 상세로 들어가고, 옆 카드를 탭하면 그 카드를
    /// 가운데로 굴린다(선택은 그 결과로 따라온다). Android `RouteCard`의 onClick과 같다.
    private func courseCarousel(ready: CourseRecommendResultReady) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: RunvealSpacing.space16) {
                ForEach(ready.routes) { route in
                    RunvealCourseCard(
                        name: CourseRecommendStrings.courseName(route.courseLabel),
                        tags: route.tags.map(CourseRecommendStrings.tagLabel),
                        distanceValue: route.distanceValue,
                        distanceUnit: route.distanceUnit,
                        timeValue: route.timeValue,
                        timeUnit: route.timeUnit,
                        elevationValue: route.elevationValue,
                        elevationUnit: route.elevationUnit,
                        newRoadValue: route.newRoadValue,
                        newRoadUnit: route.newRoadUnit,
                        state: route.routeId == ready.selectedRouteId ? .selected : .default
                    ) {
                        // 가운데(선택된) 카드를 또 탭하면 상세로, 아니면 가운데로 굴리기만 한다
                        // — 선택 자체는 센터 감지가 맡는다.
                        if route.routeId == ready.selectedRouteId {
                            onCourseClick(route.routeId)
                        } else {
                            withAnimation { centeredRouteId = route.routeId }
                        }
                    }
                    .id(route.routeId)
                }
            }
            // 좌우 여백은 화면 마진 그대로 — 가운데 정렬은 패딩이 아니라 스냅이 맡는다.
            .padding(.horizontal, RunvealLayout.marginScreen)
            .padding(.vertical, RunvealSpacing.space16)
            .scrollTargetLayout()
        }
        .scrollTargetBehavior(
            CenteredCardSnap(
                cardWidth: Self.cardWidth,
                spacing: RunvealSpacing.space16,
                leadingInset: RunvealLayout.marginScreen
            )
        )
        // 가운데(anchor: .center)에 와 있는 카드의 id를 받아 둔다 — 스냅이 끝나면 그 카드다.
        // 첫·마지막 카드처럼 가운데까지 못 가고 끝에 붙은 상태에서도 뷰포트 한가운데가 그 카드 안에
        // 들어와서 같은 답이 나온다.
        .scrollPosition(id: $centeredRouteId, anchor: .center)
        .frame(maxWidth: .infinity)
        .frame(height: 170)
        // 가운데가 바뀌면 그게 곧 선택이다. 지도 카메라는 선택이 바뀌는 걸 보고 알아서 따라간다
        // (`onChange(of: ready?.selectedRouteId)`).
        .onChange(of: centeredRouteId) { _, next in
            guard let next, next != ready.selectedRouteId else { return }
            viewModel.onIntent(.selectRoute(routeId: next))
        }
    }
}

/**
 가장 가까운 카드를 **뷰포트 가운데**로 맞추는 스냅.

 `.viewAligned`를 그대로 쓰면 항목이 보이는 영역의 **선행(leading) 가장자리**에 붙는다. 예전에는
 좌우 패딩을 "뷰포트 − 카드"의 절반으로 줘서 결과적으로 가운데처럼 보였지만, 그러면 첫 카드 왼쪽과
 마지막 카드 오른쪽에 큰 공백이 남는다(시안과 다르다). 그래서 여백은 화면 마진으로 두고 목표 위치를
 직접 계산한다.

 **양 끝은 자르기만 하면 시안이 그대로 나온다** — 첫 카드를 가운데로 보내려면 스크롤이 음수여야
 하는데 0에서 막혀 좌측 마진에 붙고, 마지막 카드는 최대 스크롤에서 막혀 우측 마진에 붙는다.
 자르는 건 시스템에 맡기지 않고 여기서 직접 한다(범위를 벗어난 목표를 넘기면 튕겼다 돌아오는
 모양이 될 수 있다).

 카드가 하나뿐이면 스크롤 범위가 0이라 좌측 마진에 붙는다 — 그게 맞는 모양이다.
 */
private struct CenteredCardSnap: ScrollTargetBehavior {
    let cardWidth: CGFloat
    let spacing: CGFloat
    /// 콘텐츠 앞쪽 여백 — 카드 i의 왼쪽 끝이 `leadingInset + i * (cardWidth + spacing)`이다.
    let leadingInset: CGFloat

    func updateTarget(_ target: inout ScrollTarget, context: TargetContext) {
        let step = cardWidth + spacing
        guard step > 0, context.containerSize.width > 0 else { return }

        // 카드 i가 뷰포트 한가운데 오는 스크롤 위치 = base + i * step.
        let base = leadingInset + cardWidth / 2 - context.containerSize.width / 2
        let index = ((target.rect.minX - base) / step).rounded()
        let maxOffset = max(context.contentSize.width - context.containerSize.width, 0)
        target.rect.origin.x = min(max(base + index * step, 0), maxOffset)
    }
}

/**
 `.background`에 깔아 그 뷰의 **화면 절대 좌표(`.global`)**를 알려 주는 투명 판.

 크기(`geometry.size`)가 아니라 좌표가 필요해서 따로 둔다 — 지도 카메라 여백은 화면 가장자리
 기준이고 이 화면의 뷰들은 안전 영역 안쪽에 놓여서, 둘을 같은 원점으로 맞춰야 한다
 ([CourseRecommendResultScreen]의 측정 주석 참고).
 */
private struct GlobalBoundsReader: View {
    let onBounds: (CGRect) -> Void

    var body: some View {
        GeometryReader { geometry in
            let bounds = geometry.frame(in: .global)
            Color.clear
                .onAppear { onBounds(bounds) }
                .onChange(of: bounds) { _, next in onBounds(next) }
        }
    }
}

/// B0에서 고른 거리·안 가본 길 우선·필터 수를 그대로 보여주는 요약 칩 — 코스 만들기 바텀 패널과 같은
/// B0에서 고른 거리·안 가본 길 우선·필터를 그대로 보여주는 요약 칩 — 코스 만들기 바텀 패널과 같은
/// 칩이지만 지도 위에 바로 떠서 글래스(OnMap) 스타일을 쓴다.
///
/// 셋 중 아무거나 누르면 코스 조건 설정 시트가 열린다 — 어느 칩을 눌렀든 바꿀 수 있는 조건은 같다.
private struct SettingsSummaryRow: View {
    let settings: CourseRecommendSettings
    let onTap: () -> Void

    var body: some View {
        HStack(spacing: RunvealSpacing.space8) {
            RunvealChipDropdown(label: settings.distanceLabel, usage: .onMap, onClick: onTap)
            if settings.avoidVisitedPreferred {
                RunvealChipCondition(
                    checked: true,
                    label: CourseRecommendStrings.avoidVisitedPreferred,
                    usage: .onMap
                ) { _ in onTap() }
            }
            if settings.selectedFilterCount > 0 {
                RunvealChipCondition(
                    checked: true,
                    label: "\(settings.selectedFilterCount)",
                    icon: .filter,
                    usage: .onMap
                ) { _ in onTap() }
            }
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct EmptyResult: View {
    let message: String
    let settings: CourseRecommendSettings
    let onIntent: (CourseRecommendResultIntent) -> Void
    let onRetry: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: RunvealSpacing.space24) {
            RunvealEmptyState(
                title: CourseRecommendStrings.resultEmptyTitle,
                // 왜 못 찾았는지는 서버·필터 상황마다 달라서 그대로 받아 설명 줄에 쓴다.
                description: message
            ) {
                RunvealImages.emptyCourse
                    .resizable()
                    .scaledToFit()
            }
            EmptyResultHints(settings: settings, onIntent: onIntent)
            Spacer(minLength: 0)
            RunvealButtonGroup {
                RunvealButtonGroupSingle(
                    label: CourseRecommendStrings.resultEmptyAction,
                    onClick: onRetry
                )
            }
        }
    }
}

/// 안내 줄 하나 — 문구와 "누르면 할 일"을 묶어 둔다.
private struct EmptyHint: Identifiable {
    let id = UUID()
    let icon: RunvealIconSpec
    let title: String
    let description: String
    let onClick: () -> Void
}

/**
 조건을 어떻게 바꾸면 되는지 알려주는 줄(시안 B1-1 "설정변경 안내"). 각 줄은 **그 줄이 말하는 것을
 그대로 해 주는 버튼**이다 — 순환 코스는 도착지를 비워 바로 다시 받고, 거리·필터는 코스 조건 설정
 시트를 띄운다(상단 조건 칩과 같은 시트).

 이미 그렇게 되어 있어서 누를 게 없는 줄은 빼놓는다 — 도착지가 없으면(이미 순환) 순환 줄을, 고른
 필터가 없으면 필터 줄을 숨긴다. 눌러도 아무 일도 안 나는 줄을 남기면 고장으로 보인다.
 */
private struct EmptyResultHints: View {
    let settings: CourseRecommendSettings
    let onIntent: (CourseRecommendResultIntent) -> Void

    private var hints: [EmptyHint] {
        var result: [EmptyHint] = []
        if settings.endLatLon != nil {
            result.append(
                EmptyHint(
                    icon: RunvealIcons.circulation,
                    title: CourseRecommendStrings.resultEmptyHintLoopTitle,
                    description: CourseRecommendStrings.resultEmptyHintLoopDescription,
                    onClick: { onIntent(.retryAsLoopCourse) }
                )
            )
        }
        result.append(
            EmptyHint(
                icon: RunvealIcons.distance,
                title: CourseRecommendStrings.resultEmptyHintDistanceTitle,
                description: CourseRecommendStrings.resultEmptyHintDistanceDescription,
                onClick: { onIntent(.openFilterSheet) }
            )
        )
        if settings.selectedFilterCount > 0 {
            result.append(
                EmptyHint(
                    icon: RunvealIcons.filter,
                    title: CourseRecommendStrings.resultEmptyHintFilterTitle,
                    description: CourseRecommendStrings.resultEmptyHintFilterDescription,
                    onClick: { onIntent(.openFilterSheet) }
                )
            )
        }
        return result
    }

    var body: some View {
        let colors = RunvealTheme.colors
        let rows = hints
        VStack(spacing: 0) {
            ForEach(Array(rows.enumerated()), id: \.element.id) { index, hint in
                RunvealListItem(
                    title: hint.title,
                    description: hint.description,
                    leadingIcon: hint.icon,
                    size: .l,
                    showDivider: index != rows.count - 1,
                    onClick: hint.onClick
                )
            }
        }
        .padding(.horizontal, RunvealSpacing.space16)
        .background(colors.surface.default, in: RunvealShapes.radius16)
        .overlay { RunvealShapes.radius16.stroke(colors.border.default, lineWidth: 1) }
    }
}
