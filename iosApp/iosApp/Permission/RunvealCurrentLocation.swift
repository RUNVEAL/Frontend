import CoreLocation
import SharedLogic

/// 현재 위치를 한 번만 받아 온다 — Android `CourseCreateScreen.fetchCurrentLocationOrNull`에 해당.
///
/// 권한을 먼저 확인하고 없으면 조용히 nil을 돌려준다(위치 서비스가 꺼져 있어 못 받아도 nil).
/// 호출부는 nil이면 기본 좌표로 떨어진다.
final class RunvealCurrentLocation: NSObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    private var continuation: CheckedContinuation<LatLon?, Never>?

    /// 위치를 못 받고 매달려 있지 않게 두는 상한.
    private static let timeout: TimeInterval = 5

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
    }

    func fetch() async -> LatLon? {
        let status = manager.authorizationStatus
        guard status == .authorizedWhenInUse || status == .authorizedAlways else { return nil }

        return await withCheckedContinuation { continuation in
            self.continuation = continuation
            manager.requestLocation()
            DispatchQueue.main.asyncAfter(deadline: .now() + Self.timeout) { [weak self] in
                self?.resume(with: nil)
            }
        }
    }

    private func resume(with location: LatLon?) {
        guard let continuation else { return }
        self.continuation = nil
        continuation.resume(returning: location)
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        resume(with: locations.last.map { LatLon(lat: $0.coordinate.latitude, lon: $0.coordinate.longitude) })
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        resume(with: nil)
    }
}
