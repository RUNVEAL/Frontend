import Foundation
import SharedLogic

/// 홈 지도가 그리는 선 레이어와 지역 오버레이를 들고 있는 모델 — Android `HomeScreen.HomeMapLayer`의
/// 상태 부분에 해당한다.
///
/// - "가본 길"은 러닝 기록이 추가될 때마다(`TrackResultHolder`) DB에서 다시 읽는다.
/// - 러닝 중에는 누적 "가본 길" 대신 지금 달리는 실시간 선을 1초마다 다시 분류해 그린다.
/// - 동/시군구/시도 오버레이는 스플래시가 미리 만들어 둔 것을 쓰고, 러닝을 끝냈을 때만 다시 만든다.
@MainActor
final class HomeMapDataModel: ObservableObject {
    /// 지도 소스 ID → GeoJSON. 지도 뷰에 그대로 넘긴다.
    @Published private(set) var layers: [String: String] = [:]
    @Published private(set) var overlay: RunvealDongOverlayBundle?
    @Published private(set) var isRecordingOrPaused = false

    private var recordingSubscription: FlowSubscription?
    private var trackResultSubscription: FlowSubscription?
    private var liveRunTask: Task<Void, Never>?
    private var latestTrackId: String?

    private static let liveRunTickInterval: Duration = .seconds(1)

    init() {
        layers = [
            RunvealMapConfig.sourceVisitedRoad: IosHomeMapData.shared.emptyGeoJson,
            RunvealMapConfig.sourceLiveRunOld: IosHomeMapData.shared.emptyGeoJson,
            RunvealMapConfig.sourceLiveRunNew: IosHomeMapData.shared.emptyGeoJson,
        ]
        overlay = RunvealDongOverlayHolder.shared.bundle
        RunvealTrace.step(.homeMapDataInit)
    }

    func start() {
        observeRecording()
        observeTrackResult()
        reloadVisitedRoad()
    }

    func stop() {
        recordingSubscription?.cancel()
        recordingSubscription = nil
        trackResultSubscription?.cancel()
        trackResultSubscription = nil
        liveRunTask?.cancel()
        liveRunTask = nil
    }

    /// 코스 추천(만들기/결과) 흐름처럼, 녹화 중이 아니어도 동/시군구/시도 레이어를 숨겨야 하는
    /// 화면에서는 오버레이를 nil로 넘긴다 — 지금 고르고 있는 코스에만 집중하게 한다.
    func visibleOverlay(hideRegionOverlays: Bool) -> RunvealDongOverlayBundle? {
        (isRecordingOrPaused || hideRegionOverlays) ? nil : overlay
    }

    // MARK: - 가본 길

    private func reloadVisitedRoad() {
        Task.detached(priority: .userInitiated) {
            let geoJson = IosHomeMapData.shared.visitedRoadGeoJson()
            await MainActor.run { self.layers[RunvealMapConfig.sourceVisitedRoad] = geoJson }
        }
    }

    // MARK: - 실시간 선

    private func observeRecording() {
        recordingSubscription = RunvealFlowWatchers.shared.watchRecording { [weak self] snapshot in
            guard let self else { return }
            let active = snapshot.isRecordingOrPaused
            guard active != self.isRecordingOrPaused else { return }
            self.isRecordingOrPaused = active
            if active {
                self.startLiveRunLoop()
            } else {
                self.stopLiveRunLoop()
            }
        }
    }

    private func startLiveRunLoop() {
        // 기록 중엔 누적 "가본 길" 대신 지금 달리는 선만 보여준다.
        layers[RunvealMapConfig.sourceVisitedRoad] = IosHomeMapData.shared.emptyGeoJson
        liveRunTask?.cancel()
        liveRunTask = Task { [weak self] in
            // 직전 러닝에서 적립된 구간까지 반영해 "이미 가본 길"을 다시 읽는다 — 안 그러면 앱을
            // 켠 채 두 번째로 달릴 때 방금 달린 길이 또 처음 가는 길(금색)로 보인다.
            await Task.detached(priority: .userInitiated) {
                IosHomeMapData.shared.refreshExploredSegments()
            }.value
            while !Task.isCancelled {
                let result = await Task.detached(priority: .userInitiated) {
                    IosHomeMapData.shared.classifyLiveRun()
                }.value
                guard let self, !Task.isCancelled else { return }
                self.layers[RunvealMapConfig.sourceLiveRunOld] = result.oldGeoJson
                self.layers[RunvealMapConfig.sourceLiveRunNew] = result.newGeoJson
                try? await Task.sleep(for: Self.liveRunTickInterval)
            }
        }
    }

    private func stopLiveRunLoop() {
        liveRunTask?.cancel()
        liveRunTask = nil
        layers[RunvealMapConfig.sourceLiveRunOld] = IosHomeMapData.shared.emptyGeoJson
        layers[RunvealMapConfig.sourceLiveRunNew] = IosHomeMapData.shared.emptyGeoJson
        reloadVisitedRoad()
    }

    // MARK: - 지역 오버레이

    /// 러닝을 끝냈을 때만(= 담긴 묶음이 그 기록을 아직 반영 못 했을 때만) 오버레이를 다시 만들어
    /// 홀더를 갈아 끼운다. 그 외에는 스플래시가 미리 만든 걸 그대로 쓴다.
    private func observeTrackResult() {
        trackResultSubscription = RunvealFlowWatchers.shared.watchTrackResult { [weak self] result in
            guard let self else { return }
            self.latestTrackId = result?.trackId
            self.reloadVisitedRoad()

            let current = RunvealDongOverlayHolder.shared.bundle
            if current != nil, current?.builtForTrackId == result?.trackId {
                self.overlay = current
                return
            }
            Task { [trackId = result?.trackId] in
                do {
                    let bundle = try await RunvealDongOverlayLoader.load(builtForTrackId: trackId)
                    RunvealDongOverlayHolder.shared.publish(bundle)
                    self.overlay = bundle
                } catch {
                    print("[HomeMap] 동 오버레이 재생성 실패: \(error)")
                }
            }
        }
    }
}
