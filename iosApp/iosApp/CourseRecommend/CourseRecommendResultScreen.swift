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
        .runvealMapLayers(mapState, routeLayers)
        .runvealMapDotMarkers(mapState, courseMarkers, owner: dotMarkersOwner)
        // 선택된 코스가 바뀌면 그 코스가 전부 보이게 카메라를 맞춘다.
        .onChange(of: ready?.selectedRouteId) { _, _ in focusSelected() }
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
        mapState.focusOnBounds(points: selected.path)
    }

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

    /// 화면 가운데에 카드 하나가 오게 좌우 패딩을 "뷰포트 - 카드" 절반씩 준다.
    ///
    /// Android는 "화면 가운데에 와 있는 카드"가 선택 상태를 정하지만(snapFling + 센터 감지), iOS는
    /// `ScrollView`가 보이는 항목 위치를 알려주지 않아서 `scrollTargetBehavior(.viewAligned)`로
    /// 스냅만 맞추고 선택은 **카드 탭**으로 정한다 — 가운데 카드를 다시 탭하면 상세로 들어간다.
    private func courseCarousel(ready: CourseRecommendResultReady) -> some View {
        GeometryReader { geometry in
            let sidePadding = max((geometry.size.width - Self.cardWidth) / 2, RunvealLayout.marginScreen)
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
                            // 이미 선택된 카드를 또 탭하면 상세로, 아니면 선택만 바꾼다.
                            if route.routeId == ready.selectedRouteId {
                                onCourseClick(route.routeId)
                            } else {
                                viewModel.onIntent(.selectRoute(routeId: route.routeId))
                            }
                        }
                        .id(route.routeId)
                    }
                }
                .padding(.horizontal, sidePadding)
                .padding(.vertical, RunvealSpacing.space16)
                .scrollTargetLayout()
            }
            .scrollTargetBehavior(.viewAligned)
            .frame(maxWidth: .infinity)
        }
        .frame(height: 170)
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
