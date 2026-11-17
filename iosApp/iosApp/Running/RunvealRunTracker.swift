import CoreLocation
import Foundation
import SharedLogic

/// 러닝 기록의 위치 수신 담당 — Android `TrackingService`의 **플랫폼 부분**에 해당한다.
///
/// Android는 Foreground Service + 알림으로 화면이 꺼져도 기록을 잇지만, iOS는
/// `allowsBackgroundLocationUpdates`(Info.plist `UIBackgroundModes: location`)로 같은 일을 한다 —
/// 그래서 알림 채널/알림 갱신 코드가 없다.
///
/// 받은 점을 넣고 나면 나머지(실시간 매칭, 종료 시 저장·최종 매칭·결과 publish)는 전부
/// sharedLogic [IosRunSession]이 처리한다.
@MainActor
final class RunvealRunTracker: NSObject, ObservableObject, CLLocationManagerDelegate {
    static let shared = RunvealRunTracker()

    /// GPS 수신 최소 간격 — Android도 5초다(문서 원안의 1초에서 배터리·데이터량 고려해 조정).
    /// iOS `CLLocationManager`에는 "요청 간격" 설정이 없어서 들어온 점을 이 간격으로 걸러낸다.
    private static let minUpdateInterval: TimeInterval = 5

    private let manager = CLLocationManager()
    private var session: IosRunSession?
    private var lastAcceptedAt: TimeInterval = 0

    private override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.activityType = .fitness
        manager.pausesLocationUpdatesAutomatically = false
    }

    /// 러닝을 시작한다. 위치 권한은 이미 승인됐다고 전제한다(권한 흐름은
    /// [RunvealLocationPermissionState]가 먼저 거친다).
    func start() {
        let session = ensureSession()
        session?.start(nowEpochMs: Self.nowEpochMs)
        startLocationUpdates()
    }

    func pause() {
        manager.stopUpdatingLocation()
        session?.pause(nowEpochMs: Self.nowEpochMs)
    }

    func resume() {
        session?.resume(nowEpochMs: Self.nowEpochMs)
        startLocationUpdates()
    }

    /// [saveRecord]가 false면 100m·1분을 못 채운 러닝이라 기록으로 남기지 않는다
    /// (판단은 `RecordingSavePolicy`).
    func stop(saveRecord: Bool) {
        manager.stopUpdatingLocation()
        session?.stop(nowEpochMs: Self.nowEpochMs, saveRecord: saveRecord)
    }

    private func startLocationUpdates() {
        if manager.authorizationStatus == .authorizedAlways {
            manager.allowsBackgroundLocationUpdates = true
        }
        lastAcceptedAt = 0
        manager.startUpdatingLocation()
    }

    /// 번들 계층 에셋이 필요해서 첫 시작 때 한 번 만든다 — 읽기에 실패하면 기록을 시작하지 않는다
    /// (동 이름을 못 찾아 결과 화면의 동별 탐험률을 만들 수 없다).
    ///
    /// 동 경계(4.3MB)는 더 이상 필요 없다 — 이름은 계층 파일(150KB)에서 꺼내고, 적립에 쓰는 경계는
    /// 서버에서 받는다(`IosServerDongData`).
    private func ensureSession() -> IosRunSession? {
        if let session { return session }
        guard let hierarchy = try? RunvealBundleAsset.text(RunvealBundleAsset.koreaRegionHierarchy) else {
            assertionFailure("계층 에셋을 읽지 못했다 — generateRunvealIosResources가 돌았는지 확인한다.")
            return nil
        }
        let session = IosRunSession(regionHierarchyJson: hierarchy)
        self.session = session
        return session
    }

    private static var nowEpochMs: Int64 {
        Int64(Date().timeIntervalSince1970 * 1000)
    }

    // MARK: - CLLocationManagerDelegate

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let session, let location = locations.last else { return }
        let now = location.timestamp.timeIntervalSince1970
        guard now - lastAcceptedAt >= Self.minUpdateInterval else { return }
        lastAcceptedAt = now

        session.addLocation(
            lat: location.coordinate.latitude,
            lon: location.coordinate.longitude,
            accuracyMeters: Float(max(location.horizontalAccuracy, 0)),
            speedMps: Float(max(location.speed, 0)),
            timeEpochMs: Int64(now * 1000)
        )
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        // 일시적인 실패는 다음 업데이트에서 복구된다 — Android도 따로 처리하지 않는다.
    }
}
