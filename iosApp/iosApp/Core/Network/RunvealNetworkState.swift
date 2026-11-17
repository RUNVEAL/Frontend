import Foundation
import Network

/// 기기의 인터넷 연결 상태. 와이파이·모바일 데이터가 **둘 다 없는 상태**를 화면에서 가려내는 데
/// 쓴다 — 서버가 죽었거나 특정 API가 503인 경우(`docs/api/API.md` §5.7)는 여기서 다루지 않는다.
/// 그건 호출한 화면이 응답을 보고 따로 처리할 몫이다.
///
/// Android `core:devicestate`의 `NetworkState`에 해당한다. `NWPathMonitor`를 구독하므로 비행기
/// 모드를 켜거나 와이파이를 끄면 **화면을 떠나지 않아도** 즉시 반영된다.
///
/// **Android와 다른 점.** Android는 `NET_CAPABILITY_VALIDATED`까지 보고 "와이파이에 붙어 있지만
/// 실제로 인터넷이 안 되는" 경우(로그인이 필요한 공공 와이파이 등)를 연결 없음으로 가려낸다.
/// iOS의 `NWPath`에는 그에 대응하는 검증 플래그가 없어 `.satisfied`만 본다 — 그런 와이파이에서는
/// "연결됨"으로 보이고 요청만 실패한다. 그 상태는 호출한 화면이 응답을 보고 처리해야 한다.
/// 반대로 Android에만 있는 부작용(막 붙은 네트워크가 검증을 기다리는 동안 잠깐 "연결 없음"으로
/// 보이는 것)도 iOS에는 없다.
@MainActor
final class RunvealNetworkState: ObservableObject {
    @Published private(set) var isConnected: Bool

    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "com.sloth.runveal.network-monitor")

    init() {
        // 구독을 시작하기 전에 현재 값으로 한 번 채운다 — 첫 콜백이 올 때까지 기다리면 실행 직후
        // 한 프레임 동안 오류 화면이 스쳐 보인다.
        isConnected = monitor.currentPath.status == .satisfied

        monitor.pathUpdateHandler = { [weak self] path in
            // 콜백은 위 전용 큐에서 온다 — @Published는 메인에서만 건드린다.
            let connected = path.status == .satisfied
            Task { @MainActor in
                self?.isConnected = connected
            }
        }
        monitor.start(queue: queue)
    }

    deinit {
        monitor.cancel()
    }

    /// 지금 연결 상태를 다시 읽어 ``isConnected``에 반영한다. "다시 시도" 버튼이 쓴다.
    ///
    /// 연결이 돌아오면 `pathUpdateHandler`가 알아서 갱신하므로 꼭 필요한 건 아니지만, 버튼을 누른
    /// 사람에게 즉시 결과를 보여주는 쪽이 낫다.
    func refresh() {
        isConnected = monitor.currentPath.status == .satisfied
    }
}
