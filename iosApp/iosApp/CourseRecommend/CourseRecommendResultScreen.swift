import SharedLogic
import SwiftUI

/// B1 — `/api/v1/routes` 응답을 지도 위 오버레이로 보여주는 결과 화면. 지도(Map Shell)는 그대로 보이고
/// 이 위에 앱바와 하단 코스 카드 리스트만 얹는다 — 배경을 깔지 않는다.
/// Android `CourseRecommendResultScreen`에 해당한다.
struct CourseRecommendResultScreen: View {
    @ObservedObject var mapState: RunvealMapState
    let onClose: () -> Void
    let onCourseClick: (String) -> Void

    @StateObject private var viewModel = CourseRecommendResultViewModel()
    @State private var dotMarkersOwner = UUID()

    /// `RunvealCourseCard`의 고정 너비(240)와 맞춰야 캐러셀 가운데 정렬 계산이 들어맞는다.
    private static let cardWidth: CGFloat = 240

    private var ready: CourseRecommendResultReady? {
        guard case .ready(let ready) = viewModel.state else { return nil }
        return ready
    }

    var body: some View {
        ZStack(alignment: .top) {
            topBar

            switch viewModel.state {
            case .loading:
                ProgressView()
                    .tint(RunvealTheme.colors.accent.default)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            case .ready(let ready):
                VStack {
                    Spacer()
                    if let message = ready.emptyMessage {
                        EmptyResult(message: message, onRetry: onClose)
                            .padding(.horizontal, RunvealLayout.marginScreen)
                            .padding(.vertical, RunvealSpacing.space16)
                    } else {
                        courseCarousel(ready: ready)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .runvealMapLayers(mapState, routeLayers)
        .runvealMapDotMarkers(mapState, courseMarkers, owner: dotMarkersOwner)
        // 선택된 코스가 바뀌면 그 코스가 전부 보이게 카메라를 맞춘다.
        .onChange(of: ready?.selectedRouteId) { _, _ in focusSelected() }
        .onAppear { focusSelected() }
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
                style: .map,
                title: CourseRecommendStrings.resultTitle,
                onLeadingClick: onClose
            )
            if let ready {
                SettingsSummaryRow(settings: ready.settings)
                    .padding(.horizontal, RunvealLayout.marginScreen)
                    .padding(.vertical, RunvealSpacing.space12)
            }
        }
        .frame(maxWidth: .infinity)
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
/// 칩이지만 지도 위에 바로 떠서 글래스(OnMap) 스타일을 쓴다. 조회용이라 탭해도 아무 동작이 없다.
private struct SettingsSummaryRow: View {
    let settings: CourseRecommendSettings

    var body: some View {
        HStack(spacing: RunvealSpacing.space8) {
            RunvealChipDropdown(label: settings.distanceLabel, usage: .onMap) {}
            if settings.avoidVisitedPreferred {
                RunvealChipCondition(
                    checked: true,
                    label: CourseRecommendStrings.avoidVisitedPreferred,
                    usage: .onMap
                ) { _ in }
            }
            if settings.selectedFilterCount > 0 {
                RunvealChipCondition(
                    checked: true,
                    label: "\(settings.selectedFilterCount)",
                    icon: .filter,
                    usage: .onMap
                ) { _ in }
            }
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct EmptyResult: View {
    let message: String
    let onRetry: () -> Void

    var body: some View {
        let colors = RunvealTheme.colors
        VStack(spacing: RunvealSpacing.space20) {
            Text(message)
                .runvealTextStyle(RunvealTheme.typography.bodyLRegular)
                .foregroundStyle(colors.text.secondary)
                .multilineTextAlignment(.center)
            RunvealButton(
                label: CourseRecommendStrings.resultEmptyAction,
                style: .gold,
                size: .l,
                onClick: onRetry
            )
        }
        .padding(RunvealSpacing.space24)
        .background(colors.surface.raised, in: RunvealShapes.radius16)
    }
}
