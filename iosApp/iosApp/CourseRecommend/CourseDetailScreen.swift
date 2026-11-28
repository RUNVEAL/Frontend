import SharedLogic
import SwiftUI

/// B2 — 추천 코스 결과에서 카드를 눌러 들어가는 코스 상세. 지도(Map Shell)는 그대로 보이고 이 위에
/// 뒤로가기와 바텀시트만 얹는다 — 배경을 깔지 않는다. 코스는 [source]가 정한 곳에서 읽어 온다.
/// Android `CourseDetailScreen`에 해당한다.
struct CourseDetailScreen: View {
    @ObservedObject var mapState: RunvealMapState
    /// 추천 결과에서 왔는지, "내 코스"에서 왔는지 — 읽어 오는 곳이 달라진다.
    let source: CourseDetailSource
    let onBack: () -> Void
    let onStartRunning: () -> Void

    @StateObject private var viewModel = CourseDetailViewModel()
    @State private var dotMarkersOwner = UUID()
    /// 바텀시트가 화면 아래쪽을 덮는 만큼, 그 실제 높이를 재서 카메라 포커싱 때 아래 여백으로 더 줘야
    /// 코스가 안 덮이는 위쪽(보이는 영역)에 맞춰진다. 처음엔 아직 못 재서 0 — 측정되면 한 번 더 맞춘다.
    @State private var sheetHeight: CGFloat = 0

    private var ready: CourseDetailReady? {
        guard case .ready(let ready) = viewModel.state else { return nil }
        return ready
    }

    var body: some View {
        ZStack(alignment: .top) {
            RunvealTopAppBar(leading: .back, style: .map, onLeadingClick: onBack)
                .frame(maxWidth: .infinity)

            switch viewModel.state {
            case .loading:
                ProgressView()
                    .tint(RunvealTheme.colors.accent.default)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            case .notFound:
                Text(CourseRecommendStrings.detailNotFound)
                    .runvealTextStyle(RunvealTheme.typography.bodyLRegular)
                    .foregroundStyle(RunvealTheme.colors.text.secondary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            case .ready(let ready):
                VStack {
                    Spacer()
                    detailSheet(ready: ready)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .runvealMapLayers(mapState, routeLayers)
        .runvealMapDotMarkers(mapState, courseMarkers, owner: dotMarkersOwner)
        .task { viewModel.onIntent(.load(source: source)) }
        .onChange(of: sheetHeight) { _, _ in focusCourse() }
        .onChange(of: ready?.path.count) { _, _ in focusCourse() }
    }

    // MARK: - 지도

    private var routeLayers: [String: String] {
        guard let ready, ready.path.count >= 2 else { return [:] }
        let route = IosRecommendedRoute(id: source.id, points: ready.path, selected: true)
        return [
            RunvealMapConfig.sourceCourseDetailRoute: IosRouteGeoJson.shared.recommendedRoutes(routes: [route])
        ]
    }

    private var courseMarkers: [MapPin] {
        guard let ready, ready.path.count >= 2 else { return [] }
        var markers: [MapPin] = []
        if let start = ready.path.first {
            markers.append(MapPin(location: start, type: .start))
        }
        if !ready.isLoop, let end = ready.path.last {
            markers.append(MapPin(location: end, type: .end))
        }
        return markers
    }

    private func focusCourse() {
        guard let ready, ready.path.count >= 2 else { return }
        // 바텀시트가 덮는 만큼 아래를 더 비운다 — 기본 여백은 그대로 두고 시트 높이만 더한다.
        mapState.focusOnBounds(
            points: ready.path,
            bottomPadding: RunvealMapState.boundsFocusPadding + sheetHeight
        )
    }

    // MARK: - 바텀시트

    private func detailSheet(ready: CourseDetailReady) -> some View {
        GeometryReader { geometry in
            // 시트가 화면을 다 덮지 않게 85%로 제한하고, "경로 정보" 목록만 그 안에서 스크롤한다 —
            // 통계·그래프는 항상 고정으로 보이고, 경유지가 많아져도 버튼이 밀려서 찌그러지지 않는다.
            let overallCap = geometry.size.height * 0.85
            // "경로 정보" 목록을 뺀 나머지(핸들·헤더·통계·그래프·버튼과 그 사이 여백)의 대략적인 높이.
            let otherContentEstimate: CGFloat = 360
            let routeStepsMaxHeight = min(max(overallCap - otherContentEstimate, 80), 280)

            VStack {
                Spacer()
                RunvealBottomSheet(
                    header: {
                        // 저장된 코스는 저장해 둔 이름을 그대로 쓴다 — "코스 {이름}"으로 조합하면
                        // "코스 코스 A"가 된다.
                        Text(ready.savedCourseName ?? CourseRecommendStrings.courseName(ready.courseLabel))
                            .runvealTextStyle(RunvealTheme.typography.heading2)
                            .foregroundStyle(RunvealTheme.colors.text.primary)
                    },
                    content: {
                        VStack(alignment: .leading, spacing: RunvealSpacing.space16) {
                            StatsRow(state: ready)
                            ElevationChart(points: ready.elevationProfile)
                            RouteStepsSection(steps: ready.routeSteps, maxListHeight: routeStepsMaxHeight)
                        }
                    },
                    footer: {
                        RunvealButtonGroup {
                            RunvealButtonGroupSecondaryPrimary(
                                secondaryLabel: ready.isSaved
                                    ? CourseRecommendStrings.savedLabel
                                    : CourseRecommendStrings.saveAction,
                                onSecondaryClick: { viewModel.onIntent(.saveCourse) },
                                primaryLabel: CourseRecommendStrings.startRunningAction,
                                onPrimaryClick: {
                                    // 코스 경로를 기록 화면에 넘기기만 한다.
                                    RunvealAnalyticsLogger.logCourseStart(fromRecommend: true)
                                    SelectedCourseHolder.shared.publish(path: ready.path, isLoop: ready.isLoop)
                                    onStartRunning()
                                }
                            )
                        }
                    }
                )
                .frame(maxHeight: overallCap)
                .background(
                    GeometryReader { sheetGeometry in
                        Color.clear.onAppear { sheetHeight = sheetGeometry.size.height }
                    }
                )
            }
        }
    }
}

private struct StatsRow: View {
    let state: CourseDetailReady

    var body: some View {
        HStack(alignment: .top, spacing: RunvealSpacing.space16) {
            RunvealStat(
                value: state.distanceValue,
                label: RunvealDesignSystemStrings.courseCardStatDistance,
                unit: state.distanceUnit,
                type: .large
            )
            .frame(maxWidth: .infinity, alignment: .leading)
            RunvealStat(
                value: state.timeValue,
                label: RunvealDesignSystemStrings.courseCardStatTime,
                unit: state.timeUnit,
                type: .large
            )
            .frame(maxWidth: .infinity, alignment: .leading)
            RunvealStat(
                value: state.elevationValue,
                label: RunvealDesignSystemStrings.courseCardStatElevation,
                unit: state.elevationUnit,
                type: .large
            )
            .frame(maxWidth: .infinity, alignment: .leading)
            RunvealStat(
                value: state.newRoadValue,
                label: RunvealDesignSystemStrings.courseCardStatNewRoad,
                unit: state.newRoadUnit,
                type: .large,
                accent: true
            )
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct RouteStepsSection: View {
    let steps: [RouteStepUiModel]
    let maxListHeight: CGFloat

    var body: some View {
        VStack(alignment: .leading, spacing: RunvealSpacing.space8) {
            Text(CourseRecommendStrings.routeInfoTitle)
                .runvealTextStyle(RunvealTheme.typography.bodyLSemibold)
                .foregroundStyle(RunvealTheme.colors.text.secondary)

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(steps.enumerated()), id: \.element.id) { index, step in
                        RunvealRouteStep(
                            place: step.place,
                            distance: step.distanceLabel,
                            type: step.type,
                            note: step.note,
                            showConnector: index != steps.count - 1
                        )
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .frame(maxHeight: maxListHeight)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
