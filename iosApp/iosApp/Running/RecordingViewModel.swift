import Foundation
import SharedLogic

/// Android `RecordingViewModel`에 해당한다.
@MainActor
final class RecordingViewModel: MviViewModel<RecordingIntent, RecordingScreenState, RecordingEffect> {
    private static let tickerInterval: Duration = .seconds(1)

    private var recordingSubscription: FlowSubscription?
    private var newRoadSubscription: FlowSubscription?

    init() {
        // "이 코스로 러닝 시작"으로 넘어온 코스를 한 번만 꺼내 초기 상태에 담는다 — 홀더는 읽는 순간
        // 비워지므로(`SelectedCourseHolder.consume`) 생성 시점에 딱 한 번만 읽어야 한다.
        let selected = SelectedCourseHolder.shared.consume()
        var initial = RecordingScreenState()
        initial.selectedCoursePath = selected?.path
        initial.selectedCourseIsLoop = selected?.isLoop ?? false
        super.init(initialState: initial)

        // 러닝 기록 화면 진입 — 뷰모델이 만들어지는 이 시점이 곧 화면이 열린 시점이고, 들어온
        // 길(자유 러닝/코스)도 위에서 홀더에서 꺼낸 값으로 알 수 있다.
        //
        // 네비게이션 호출부가 아니라 여기서 쏘는 이유는 Android와 같다 — 기록 화면으로 오는 길이
        // 여럿이라 호출부마다 심으면 새 길이 생길 때 조용히 빠진다.
        RunvealAnalyticsLogger.logRunStart(hasCourse: initial.selectedCoursePath != nil)

        observeRecording()
        observeNewRoad()
        startTicker()
    }

    deinit {
        recordingSubscription?.cancel()
        newRoadSubscription?.cancel()
    }

    func onDisappear() {
        recordingSubscription?.cancel()
        recordingSubscription = nil
        newRoadSubscription?.cancel()
        newRoadSubscription = nil
        cancelRunningTasks()
    }

    override func handleIntent(_ intent: RecordingIntent) async {
        switch intent {
        case .startClicked:
            sendEffect(.startTracking)
        case .pauseClicked:
            sendEffect(.pauseTracking)
        case .resumeClicked:
            sendEffect(.resumeTracking)
        case .stopRequested:
            setState { state in
                var next = state
                next.finishDialog = RecordingFinishDialog(willSaveRecord: Self.isRecordSavable())
                return next
            }
        case .stopDismissed:
            setState { state in
                var next = state
                next.finishDialog = nil
                return next
            }
        case .stopClicked:
            setState { state in
                var next = state
                next.finishDialog = nil
                return next
            }
            // 저장 여부는 여기서 한 번만 정해 트래커까지 그대로 넘긴다 — 트래커가 따로 다시 판단하면
            // 그 사이 1분/100m 경계를 넘겨서 "홈으로 돌려보냈는데 기록은 저장됨" 같은 어긋남이 생긴다.
            sendEffect(.stopTracking(saveRecord: Self.isRecordSavable()))
        }
    }

    /// 지금 끝내면 기록이 저장되는지(100m·1분). 다이얼로그 문구와 실제 저장 판정이 같은 값을 쓰도록
    /// 한 곳에 둔다(Android `RecordingViewModel.isRecordSavable`).
    private static func isRecordSavable() -> Bool {
        RecordingSavePolicy.shared.isSavable(
            distanceMeters: RunvealRecorder.shared.totalDistanceMeters(),
            elapsedMs: RunvealRecorder.shared.elapsedMs(nowEpochMs: nowEpochMs)
        )
    }

    private func observeRecording() {
        recordingSubscription = RunvealFlowWatchers.shared.watchRecording { [weak self] snapshot in
            guard let self else { return }
            self.setState { state in
                var next = state
                switch snapshot.phase {
                case .recording:
                    next.phase = .recording
                case .paused:
                    next.phase = .paused
                default:
                    // 코스 데이터(selectedCoursePath/selectedCourseIsLoop)는 그대로 들고 있는다 —
                    // 전체 리셋을 쓰면 생성 시점에 미리 읽어 둔 값이 지워진다.
                    next = RecordingScreenState(
                        selectedCoursePath: state.selectedCoursePath,
                        selectedCourseIsLoop: state.selectedCourseIsLoop
                    )
                }
                return next
            }
        }
    }

    /// "새로 탐험한 길"은 지도(홈 셸)가 1초마다 돌리는 분류 결과를 그대로 받아 쓴다 — 여기서 또
    /// 분류하면 전국 구간 인덱스를 매초 두 번 훑게 되고, 지도 금색 선과 배지 숫자가 서로 다른
    /// 계산을 근거로 삼게 된다.
    private func observeNewRoad() {
        newRoadSubscription = RunvealFlowWatchers.shared.watchLiveNewRoadMeters { [weak self] meters in
            guard let self else { return }
            let label = RunFormat.kilometers(meters.doubleValue)
            self.setState { state in
                var next = state
                next.newRoadLabel = label
                return next
            }
        }
    }

    /// 기록 상태는 위치가 들어올 때만 갱신되므로, 경과 시간/페이스 표시는 따로 1초마다 갈아준다.
    /// 일시정지 중에도 돌긴 하지만 `elapsedMs`가 멈춘 시점에 고정값을 주므로 화면엔 숫자가 그대로
    /// 멈춰 보인다.
    private func startTicker() {
        launch { [weak self] in
            while !Task.isCancelled {
                guard let self else { return }
                let snapshot = RunvealRecorder.shared.snapshot()
                if snapshot.isRecordingOrPaused {
                    let elapsedMs = RunvealRecorder.shared.elapsedMs(nowEpochMs: Self.nowEpochMs)
                    let distanceMeters = RunvealRecorder.shared.totalDistanceMeters()
                    self.setState { state in
                        var next = state
                        next.elapsedLabel = RunFormat.elapsed(elapsedMs)
                        next.distanceLabel = RunFormat.kilometers(distanceMeters)
                        next.paceLabel = RunFormat.pace(elapsedMs: elapsedMs, distanceMeters: distanceMeters)
                        return next
                    }
                }
                try? await Task.sleep(for: Self.tickerInterval)
            }
        }
    }

    private static var nowEpochMs: Int64 {
        Int64(Date().timeIntervalSince1970 * 1000)
    }
}
