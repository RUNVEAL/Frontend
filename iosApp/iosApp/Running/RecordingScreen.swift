import SharedLogic
import SwiftUI

/// "기록 시작/종료" 오버레이 — Map Shell 위에 얹힌다(`recording-lifecycle.md` §3/§5).
///
/// 이 화면에 들어오는 경로는 홈 지도의 "러닝 시작" 버튼, 또는 코스 상세의 "이 코스로 러닝 시작"
/// 버튼 — 어느 쪽이든 진입 자체를 "시작" 의도로 본다. 별도의 "시작" 버튼을 다시 보여주지 않고
/// 나타날 때 바로 기록을 시작한다. 뒤로가기(스와이프)로는 못 벗어난다 — 종료는 "종료하기" 버튼으로만.
struct RecordingScreen: View {
    @ObservedObject var mapState: RunvealMapState
    let onFinished: () -> Void
    /// 너무 짧아서 저장하지 않은 러닝 — 결과 화면 없이 홈으로 돌아가는 경로.
    let onDiscarded: () -> Void

    @StateObject private var viewModel = RecordingViewModel()
    @State private var courseProgress: RecordingCourseProgress?
    /// `.runvealMapDotMarkers`가 "내가 올린 마커만 치우기"를 판단하는 표식.
    @State private var courseMarkersOwner = UUID()
    /// 하단 패널이 지도를 가리는 높이 — 그만큼 카메라 아래 여백을 줘서 현재 위치가 패널 위
    /// 보이는 영역의 가운데에 오게 한다. 패널 높이는 기록/일시정지·코스 진행 표시 유무로 달라져서
    /// 실제로 재서 넘긴다(Android `onSizeChanged`와 같은 이유).
    @State private var panelHeight: CGFloat = 0
    /// 줌은 **시작할 때 한 번만** 맞춘다 — 재개할 때 다시 맞추면 그 사이 사용자가 보려고 조절해 둔
    /// 배율이 멋대로 되돌아간다("내 위치" 버튼도 같은 이유로 줌을 안 건드린다).
    @State private var isFirstStart = true

    private static let courseGuideTickerInterval: Duration = .seconds(1)

    var body: some View {
        ZStack(alignment: .top) {
            if viewModel.state.phase != .idle {
                VStack(alignment: .leading, spacing: RunvealSpacing.space12) {
                    // 일시정지 중에는 기록이 멈췄다는 걸 지도 위에서도 알린다(시안 C0-1) — 하단
                    // 패널의 숫자가 멈춘 것만으로는 멈췄는지 느린지 알기 어렵다(Android와 같다).
                    if viewModel.state.phase == .paused {
                        RunvealNavigationBanner(
                            icon: RunvealIcons.pause,
                            instruction: RunningStrings.pausedBannerTitle,
                            description: RunningStrings.pausedBannerDescription
                        )
                    }
                    NewRoadBadge(distanceLabel: viewModel.state.newRoadLabel)
                }
                .padding(.horizontal, RunvealLayout.marginScreen)
                .padding(.top, RunvealSpacing.space16)
                .frame(maxWidth: .infinity, alignment: .leading)
            }

            VStack {
                Spacer()
                switch viewModel.state.phase {
                case .idle:
                    // 시작 직후 기록 상태가 아직 안 바뀐 찰나 — 그릴 게 없다.
                    EmptyView()
                case .recording:
                    RecordingOverlay(
                        state: viewModel.state,
                        courseProgress: courseProgress,
                        mapState: mapState,
                        onPanelHeightChange: { panelHeight = $0 }
                    ) {
                        RunvealRunPauseButton { viewModel.onIntent(.pauseClicked) }
                    }
                case .paused:
                    RecordingOverlay(
                        state: viewModel.state,
                        courseProgress: courseProgress,
                        mapState: mapState,
                        onPanelHeightChange: { panelHeight = $0 }
                    ) {
                        RunvealButtonGroupEqual(
                            firstLabel: RunningStrings.recordingStopButton,
                            onFirstClick: { viewModel.onIntent(.stopRequested) },
                            secondLabel: RunningStrings.recordingResumeButton,
                            onSecondClick: { viewModel.onIntent(.resumeClicked) }
                        )
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        // 끝내기 전에 한 번 묻는다 — 100m·1분을 못 채운 러닝은 저장되지 않으니, 그땐 문구와 버튼을
        // "저장 없이 종료"로 바꿔 끝내기 전에 알린다(Android와 같은 분기).
        .runvealDialog(
            isPresented: viewModel.state.finishDialog != nil,
            onDismiss: { viewModel.onIntent(.stopDismissed) }
        ) {
            let willSave = viewModel.state.finishDialog?.willSaveRecord ?? true
            RunvealDialog(
                title: willSave
                    ? RunningStrings.recordingFinishDialogTitle
                    : RunningStrings.recordingFinishDialogUnsavedTitle,
                confirmLabel: willSave
                    ? RunningStrings.recordingFinishDialogConfirm
                    : RunningStrings.recordingFinishDialogUnsavedConfirm,
                cancelLabel: willSave
                    ? RunningStrings.recordingFinishDialogCancel
                    : RunningStrings.recordingFinishDialogUnsavedCancel,
                description: willSave
                    ? RunningStrings.recordingFinishDialogDescription
                    : RunningStrings.recordingFinishDialogUnsavedDescription,
                // 화면 이동은 stopTracking effect를 처리한 뒤에 한다(아래 collectEffect).
                onConfirm: { viewModel.onIntent(.stopClicked) },
                onCancel: { viewModel.onIntent(.stopDismissed) }
            )
        }
        .runvealMapLayers(mapState, courseGuideLayers)
        .runvealMapDotMarkers(mapState, courseMarkers, owner: courseMarkersOwner)
        .collectEffect(viewModel.effects) { effect in
            switch effect {
            case .startTracking:
                RunvealRunTracker.shared.start()
            case .pauseTracking:
                RunvealRunTracker.shared.pause()
            case .resumeTracking:
                RunvealRunTracker.shared.resume()
            case .stopTracking(let saveRecord):
                // 화면 전환은 트래커를 실제로 멈춘 **뒤**에 한다 — 먼저 전환하면 이 화면이 사라지면서
                // effect 구독이 끊겨 정지 요청이 전달되지 못한다.
                RunvealRunTracker.shared.stop(saveRecord: saveRecord)
                // 100m·1분을 못 채운 러닝은 저장하지 않으니 보여줄 결과도 없다 — 결과 화면을 건너뛰고
                // 홈으로 돌려보낸다.
                if saveRecord { onFinished() } else { onDiscarded() }
            }
        }
        .task {
            if viewModel.state.phase == .idle {
                viewModel.onIntent(.startClicked)
            }
            await focusCourseAndTrackProgress()
        }
        // 러닝 중 지도 카메라 — 달리는 동안엔 현재 위치를 계속 따라가고(북쪽 고정), 일시정지하면 멈춘다.
        .onChange(of: viewModel.state.phase) { _, phase in
            switch phase {
            case .recording:
                mapState.startFollowingCurrentLocation(
                    zoom: isFirstStart ? RunvealMapConfig.recordingZoom : nil
                )
                isFirstStart = false
            case .paused:
                mapState.stopFollowingCurrentLocation()
            case .idle:
                break
            }
        }
        .onChange(of: panelHeight) { _, height in mapState.followBottomInset = height }
        // 러닝 중에는 내 위치 마커에 지금 바라보는 방향을 화살표로 띄운다.
        .onAppear { mapState.showsHeadingIndicator = true }
        .onDisappear {
            // 지도(Map Shell)는 이 화면보다 오래 산다 — 나갈 때 끄지 않으면 홈 지도에도 화살표가
            // 계속 붙어 있고 지도가 계속 현재 위치를 따라다닌다. 패널만큼 비워 두던 카메라 여백도
            // 이때 같이 되돌린다(Android `RecordingScreen`의 DisposableEffect와 같은 이유).
            mapState.showsHeadingIndicator = false
            mapState.stopFollowingCurrentLocation(resetAnchor: true)
            mapState.followBottomInset = 0
            viewModel.onDisappear()
        }
    }

    // MARK: - 코스 안내선

    /// "이 코스로 러닝 시작"으로 들어왔으면 그 코스를 흰 점선 안내선으로 그린다. 안내선은 현위치와
    /// 무관하게 추천 코스 **전체**를 그대로 유지한다 — MVP 1차에서는 현위치에 따라 코스를 바꾸지 않는다.
    private var courseGuideLayers: [String: String] {
        guard let path = viewModel.state.selectedCoursePath, path.count >= 2 else { return [:] }
        return [RunvealMapConfig.sourceRecordingCourseGuide: IosRouteGeoJson.shared.courseGuide(path: path)]
    }

    /// 출발·도착 핀 — 코스 상세/추천 결과 화면과 같은 규칙으로, 순환 코스는 도착이 곧 출발이라
    /// 같은 자리에 마커가 겹치지 않게 출발만 띄운다.
    private var courseMarkers: [MapPin] {
        guard let path = viewModel.state.selectedCoursePath, path.count >= 2 else { return [] }
        var markers: [MapPin] = []
        if let start = path.first {
            markers.append(MapPin(location: start, type: .start))
        }
        if !viewModel.state.selectedCourseIsLoop, let end = path.last {
            markers.append(MapPin(location: end, type: .end))
        }
        return markers
    }

    /// 러닝이 시작되는 시점에 한 번 코스 전체가 보이게 카메라를 맞추고, 그 뒤 매초 남은 거리·진행률을
    /// 계산해 패널에 넘긴다.
    ///
    /// 진행률은 "달린 거리"가 아니라 **코스를 실제로 지나간 만큼**으로 센다(`CourseProgressTracker`).
    /// 달린 거리로 세면 코스와 상관없는 곳을 달려도 진행률이 오르고, 순환 코스는 출발점과 도착점이
    /// 같은 자리라 "현위치에서 가장 가까운 점" 방식도 못 쓴다.
    private func focusCourseAndTrackProgress() async {
        guard let path = viewModel.state.selectedCoursePath, path.count >= 2 else { return }
        // 코스 전체로 카메라를 맞추지 않는다 — 시작하자마자 현재 위치 추적이 켜지므로 그 포커싱은
        // 바로 덮인다(Android도 추적만 하고 코스 포커싱은 하지 않는다).

        let tracker = IosCourseProgress.shared.tracker(coursePath: path)
        var consumedPointCount = 0
        while !Task.isCancelled {
            // 매초 "마지막 위치"만 보면 그 사이 지나친 코스 구간을 놓칠 수 있어, 아직 안 본 점을 전부 넘긴다.
            let filtered = RunvealRecorder.shared.currentFilteredPath()
            if filtered.count > consumedPointCount {
                tracker.advance(points: Array(filtered[consumedPointCount...]))
                consumedPointCount = filtered.count
            }
            courseProgress = RecordingCourseProgress(
                remainingDistanceLabel: RunFormat.kilometers(tracker.remainingMeters),
                progressPercent: Int(tracker.progressPercent)
            )
            try? await Task.sleep(for: Self.courseGuideTickerInterval)
        }
    }
}

/// 좌상단 "새로 탐험한 길" 배지 — 총 거리가 아니라 이미 가본 구간을 뺀 거리다. 지도가 금색으로
/// 칠하는 구간과 같은 분류 결과를 쓴다(전부 로컬 판정, `docs/api/API.md` §3.5).
private struct NewRoadBadge: View {
    let distanceLabel: String

    var body: some View {
        let colors = RunvealTheme.colors
        HStack(spacing: RunvealSpacing.space8) {
            Text(RunningStrings.recordingLegendNewLabel)
                .runvealTextStyle(RunvealTheme.typography.bodyMSemibold)
                .foregroundStyle(colors.text.secondary)
            Text(RunningStrings.recordingNewDistance(distanceLabel))
                .runvealTextStyle(RunvealTheme.typography.bodyMSemibold)
                .foregroundStyle(colors.text.accent)
        }
        .padding(.horizontal, RunvealSpacing.space12)
        .padding(.vertical, RunvealSpacing.space8)
        .background(colors.surface.raised, in: RunvealShapes.full)
    }
}

/// 범례+내 위치 줄 + 패널(16 간격) — 기록 중/일시정지 공통.
///
/// [onPanelHeightChange]로 패널(지도를 실제로 가리는 불투명한 부분)의 높이를 알려준다 — 러닝 중
/// 카메라가 현재 위치를 "패널 위 보이는 영역의 가운데"에 두는 데 쓴다. 범례·내 위치 줄은 지도가
/// 그대로 보이는 자리라 높이에서 뺀다.
private struct RecordingOverlay<Action: View>: View {
    let state: RecordingScreenState
    let courseProgress: RecordingCourseProgress?
    let mapState: RunvealMapState
    let onPanelHeightChange: (CGFloat) -> Void
    @ViewBuilder let action: () -> Action

    var body: some View {
        VStack(spacing: RunvealSpacing.space16) {
            MapLegendRow(
                showCourseGuide: state.selectedCoursePath != nil,
                // 러닝 중에는 한 번 옮기기가 아니라 **추적 복귀**다 — 지도를 드래그해 추적이 풀린
                // 뒤 다시 따라가게 한다(Android도 이 버튼이 startFollowing을 부른다).
                onLocateClick: { mapState.startFollowingCurrentLocation() }
            )
            .padding(.horizontal, RunvealLayout.marginScreen)

            RecordingPanel(state: state, courseProgress: courseProgress, action: action)
                .background(
                    GeometryReader { panelGeometry in
                        Color.clear
                            .onAppear { onPanelHeightChange(panelGeometry.size.height) }
                            .onChange(of: panelGeometry.size.height) { _, height in
                                onPanelHeightChange(height)
                            }
                    }
                )
        }
        .frame(maxWidth: .infinity)
    }
}

/// 지도 범례 칩([showCourseGuide]면 추천 코스 칩이 그 위에 따로 한 줄 더) + 내 위치 버튼.
private struct MapLegendRow: View {
    let showCourseGuide: Bool
    let onLocateClick: () -> Void

    /// 디자인의 locate 버튼 크기(보이는 원 44, 터치 영역 48) — `RunvealIconButton`의 M/L 어느 쪽도
    /// 배경(glass)이 정확히 맞지 않아 토큰을 직접 조합했다(Android도 같은 이유로 하드코딩).
    private static let locateVisibleSize: CGFloat = 44
    private static let locateTouchSize: CGFloat = 48

    var body: some View {
        let colors = RunvealTheme.colors
        HStack(alignment: .bottom) {
            VStack(alignment: .leading, spacing: RunvealSpacing.space4) {
                if showCourseGuide {
                    LegendChip {
                        // 지도 위 안내선과 같은 색(text.primary, 흰색) · 같은 점선.
                        LegendItem(
                            swatchColor: colors.text.primary,
                            label: RunningStrings.recordingLegendCourseLabel,
                            dashed: true
                        )
                    }
                }
                LegendChip {
                    LegendItem(swatchColor: colors.map.roadToday, label: RunningStrings.recordingLegendTodayLabel)
                    LegendItem(
                        swatchColor: colors.map.roadNew,
                        label: RunningStrings.recordingLegendNewLabel,
                        haloColor: colors.map.roadNewHalo
                    )
                }
            }

            Spacer()

            Button(action: onLocateClick) {
                RunvealIcon(RunvealIcons.locate, tint: colors.icon.primary, size: RunvealLayout.sizeIcon)
                    .frame(width: Self.locateVisibleSize, height: Self.locateVisibleSize)
                    .background(colors.surface.default, in: RunvealShapes.full)
                    .overlay { RunvealShapes.full.stroke(colors.border.default, lineWidth: 1) }
                    .frame(width: Self.locateTouchSize, height: Self.locateTouchSize)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(RunningStrings.recordingLocateAccessibilityLabel)
        }
        .frame(maxWidth: .infinity)
    }
}

/// 범례 칩 하나 — 배경(`bg.scrim`)으로 묶인 [LegendItem] 한 줄.
private struct LegendChip<Content: View>: View {
    @ViewBuilder let content: () -> Content

    var body: some View {
        HStack(spacing: RunvealSpacing.space12) {
            content()
        }
        .padding(.horizontal, RunvealSpacing.space12)
        .padding(.vertical, RunvealSpacing.space8)
        .background(RunvealTheme.colors.bg.scrim, in: RunvealShapes.radius8)
    }
}

/// 범례 한 칸 — 지도 선 색(작은 선 스와치) + 라벨. [haloColor]를 주면 실제 지도에 도로가 칠해질 때와
/// 같은 번짐 효과를 작게 얹는다. [dashed]면 실선 대신 점선 스와치(추천 코스 안내선용).
struct LegendItem: View {
    let swatchColor: Color
    let label: String
    var haloColor: Color?
    var dashed: Bool = false

    /// 바텀시트 드래그 핸들과 같은 모양이지만 12pt 라벨 옆에 쓰기엔 커서 절반 정도로 줄인 값.
    private static let swatchWidth: CGFloat = 16
    private static let swatchHeight: CGFloat = 3
    private static let haloSize: CGFloat = 10
    private static let haloPadding: CGFloat = 3
    private static let haloBlur: CGFloat = 4
    private static let labelGap: CGFloat = 6

    var body: some View {
        HStack(spacing: Self.labelGap) {
            ZStack {
                if let haloColor {
                    RunvealShapes.full
                        .fill(haloColor)
                        .frame(width: Self.swatchWidth + Self.haloPadding * 2, height: Self.haloSize)
                        .blur(radius: Self.haloBlur)
                }
                if dashed {
                    // 지도 안내선과 같은 비율(대시:간격 = 1.4:0.5) — 살짝 긴 대시, 촘촘한 간격.
                    Path { path in
                        path.move(to: CGPoint(x: 0, y: Self.swatchHeight / 2))
                        path.addLine(to: CGPoint(x: Self.swatchWidth, y: Self.swatchHeight / 2))
                    }
                    .stroke(
                        swatchColor,
                        style: StrokeStyle(
                            lineWidth: Self.swatchHeight,
                            dash: [Self.swatchHeight * 1.4, Self.swatchHeight * 0.5]
                        )
                    )
                    .frame(width: Self.swatchWidth, height: Self.swatchHeight)
                } else {
                    RunvealShapes.full
                        .fill(swatchColor)
                        .frame(width: Self.swatchWidth, height: Self.swatchHeight)
                }
            }
            Text(label)
                .runvealTextStyle(RunvealTheme.typography.captionMRegular)
                .foregroundStyle(RunvealTheme.colors.text.secondary)
        }
    }
}

/// 기록 중/일시정지 공통 패널 — 거리, 경과 시간/평균 페이스(+코스 진행), 하단 액션만 상태별로 다르다.
/// 바텀시트처럼 화면 하단에 깔리지만 드래그 핸들은 없다.
private struct RecordingPanel<Action: View>: View {
    let state: RecordingScreenState
    let courseProgress: RecordingCourseProgress?
    @ViewBuilder let action: () -> Action

    private static var actionHeight: CGFloat { 80 }

    var body: some View {
        let colors = RunvealTheme.colors
        let shape = UnevenRoundedRectangle(
            topLeadingRadius: RunvealRadius.radius24,
            bottomLeadingRadius: 0,
            bottomTrailingRadius: 0,
            topTrailingRadius: RunvealRadius.radius24
        )

        VStack(spacing: RunvealSpacing.space20) {
            VStack(spacing: RunvealSpacing.space4) {
                HStack(alignment: .lastTextBaseline, spacing: 0) {
                    Text(state.distanceLabel)
                        .runvealTextStyle(RunvealTheme.typography.numDisplay)
                        .foregroundStyle(colors.text.primary)
                    Text(RunningStrings.recordingDistanceUnit)
                        .runvealTextStyle(RunvealTheme.typography.unitDisplay)
                        .foregroundStyle(colors.text.secondary)
                }
                if let courseProgress {
                    Text(RunningStrings.recordingCourseRemainingDistance(courseProgress.remainingDistanceLabel))
                        .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                        .foregroundStyle(colors.text.secondary)
                }
            }
            .frame(maxWidth: .infinity)

            HStack(spacing: RunvealSpacing.space8) {
                RecordingStat(label: RunningStrings.recordingElapsedLabel, value: state.elapsedLabel)
                    .frame(maxWidth: .infinity)
                RecordingStat(label: RunningStrings.recordingPaceLabel, value: state.paceLabel)
                    .frame(maxWidth: .infinity)
                if let courseProgress {
                    RecordingStat(
                        label: RunningStrings.recordingCourseProgressLabel,
                        value: "\(courseProgress.progressPercent)",
                        unit: RunningStrings.recordingCourseProgressUnit
                    )
                    .frame(maxWidth: .infinity)
                }
            }
            .frame(maxWidth: .infinity)

            ZStack {
                action()
            }
            .frame(maxWidth: .infinity)
            .frame(height: Self.actionHeight)
        }
        .padding(.horizontal, RunvealLayout.marginScreen)
        .padding(.top, RunvealSpacing.space24)
        .padding(.bottom, RunvealSpacing.space32)
        .frame(maxWidth: .infinity)
        // 배경만 OS 내비게이션 바(제스처 바) 뒤까지 채운다 — `RunvealBottomSheet`와 같은 컨벤션.
        .background {
            shape.fill(colors.surface.raised).ignoresSafeArea(edges: .bottom)
        }
    }
}

/// [unit]을 주면(코스 진행의 "%") 값·단위를 한 줄로, 안 주면(경과 시간/평균 페이스) 값만.
private struct RecordingStat: View {
    let label: String
    let value: String
    var unit: String?

    var body: some View {
        let colors = RunvealTheme.colors
        VStack(spacing: RunvealSpacing.space4) {
            Text(label)
                .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                .foregroundStyle(colors.text.secondary)
            if let unit {
                HStack(alignment: .lastTextBaseline, spacing: 2) {
                    Text(value)
                        .runvealTextStyle(RunvealTheme.typography.numM)
                        .foregroundStyle(colors.text.primary)
                    Text(unit)
                        .runvealTextStyle(RunvealTheme.typography.unitM)
                        .foregroundStyle(colors.text.secondary)
                }
            } else {
                Text(value)
                    .runvealTextStyle(RunvealTheme.typography.numM)
                    .foregroundStyle(colors.text.primary)
            }
        }
    }
}
