import CoreLocation
import SwiftUI
import UIKit

/// 위치 권한 상태 — 지도에 현재 위치 점을 띄우는 데 필요한 최소 권한만 다룬다.
/// Android `LocationPermissionState`에 해당한다.
///
/// Android와 다른 점:
///  - Android는 권한(FINE/COARSE)과 기기 위치 서비스(GPS) 토글이 따로지만, iOS는 둘 다
///    `CLLocationManager`의 권한 상태와 시스템 전역 "위치 서비스" 스위치로 나뉜다.
///  - Android는 Play Services의 "위치 켜기" 확인 다이얼로그를 띄울 수 있지만 iOS에는 그런 API가
///    없어서, 위치 서비스가 꺼져 있으면 설정 앱으로 보낸다.
///  - "영구 거부"는 iOS에서 `.denied`/`.restricted` 상태가 그대로 알려 준다 — Android처럼
///    "요청한 적 있는지"를 따로 저장할 필요가 없다(한 번 거부하면 다시 못 묻는 것도 같다).
@MainActor
final class RunvealLocationPermissionState: NSObject, ObservableObject, CLLocationManagerDelegate {
    @Published private(set) var isGranted = false
    @Published private(set) var isPermanentlyDenied = false
    @Published private(set) var isLocationServicesEnabled = true

    private let manager = CLLocationManager()

    override init() {
        super.init()
        manager.delegate = self
        refresh()
        RunvealTrace.step(.locationPermissionInit)
    }

    func request() {
        manager.requestWhenInUseAuthorization()
    }

    /// iOS에는 "위치 서비스 켜기" 확인 다이얼로그 API가 없다 — 설정 앱으로 보낸다.
    func requestEnableLocationServices() {
        openSettings()
    }

    /// 앱 상세 설정 화면(권한 등)을 연다 — Android `openAppSettings`.
    func openSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(url)
    }

    /// 설정 앱에서 권한을 바꾸고 돌아오는 경우를 위해 포그라운드 복귀 때 다시 읽는다 —
    /// Android가 `ON_RESUME`에서 다시 읽는 것과 같은 이유다(`locationManagerDidChangeAuthorization`도
    /// 대부분 불리지만, 전역 위치 서비스 토글은 그 콜백을 안 거치는 경우가 있다).
    func refresh() {
        let status = manager.authorizationStatus
        isGranted = status == .authorizedWhenInUse || status == .authorizedAlways
        isPermanentlyDenied = status == .denied || status == .restricted
        // `locationServicesEnabled()`는 메인 스레드에서 블로킹될 수 있다고 경고가 있지만, 전역 토글을
        // 알 다른 방법이 없다. 값이 거의 안 바뀌므로 상태가 갱신될 때만 한 번 읽는다.
        isLocationServicesEnabled = CLLocationManager.locationServicesEnabled()
    }

    // MARK: - CLLocationManagerDelegate

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        refresh()
    }
}

/// 위치 권한 요청 흐름 전체를 여기서 끝낸다 — Android `RequireLocationPermission`에 해당한다.
///
/// 감싼 내용은 권한 유무와 상관없이 항상 그리고, 미승인 상태면 그 위에 안내 바텀시트를 띄운다.
/// 시스템 권한 다이얼로그는 진입 시 자동으로 띄우지 않고, 바텀시트의 '위치 권한 허용하기' 버튼을
/// 눌렀을 때만 띄운다. 거부해서 시스템 다이얼로그를 더 띄울 수 없으면 설정 화면으로 보내는
/// 바텀시트를 대신 띄운다.
struct RequireLocationPermissionModifier: ViewModifier {
    @ObservedObject var state: RunvealLocationPermissionState
    @State private var dismissed = false
    @Environment(\.scenePhase) private var scenePhase

    func body(content: Content) -> some View {
        content
            .onChange(of: scenePhase) { _, phase in
                if phase == .active { state.refresh() }
            }
            .sheet(isPresented: Binding(
                get: { !state.isGranted && !dismissed },
                set: { if !$0 { dismissed = true } }
            )) {
                sheetContent
                    .presentationBackground(Color.clear)
                    .presentationDragIndicator(.hidden)
            }
    }

    @ViewBuilder
    private var sheetContent: some View {
        if state.isPermanentlyDenied {
            RunvealLocationPermissionSettingsSheet(
                onSettingsClick: { state.openSettings() },
                onCloseClick: { dismissed = true }
            )
            .presentationDetents([.height(320)])
        } else {
            RunvealLocationPermissionSheet(
                onAllowClick: { state.request() },
                onLaterClick: { dismissed = true }
            )
            .presentationDetents([.height(520)])
        }
    }
}

extension View {
    /// 위치 권한 안내 바텀시트를 이 화면 위에 띄운다.
    func requireLocationPermission(_ state: RunvealLocationPermissionState) -> some View {
        modifier(RequireLocationPermissionModifier(state: state))
    }
}
