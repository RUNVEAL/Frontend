import SwiftUI

/// 어느 화면에 있든 네트워크가 끊기면 그 위를 ``NetworkErrorScreen``으로 덮는다. 화면마다 따로
/// 붙이지 않고 앱 최상위(``RunvealRootView``)에 한 번만 두면 된다.
///
/// 닫을 수 있다 — 상단 앱바의 뒤로가기를 누르면 원래 화면으로 돌아가고, 연결이 없어도 그대로 앱을
/// 쓸 수 있다. 네트워크가 필요 없는 일(지난 기록 보기, 러닝 기록 등)을 막지 않기 위해서다. 닫은
/// 뒤에는 **연결이 돌아왔다가 다시 끊길 때** 다시 뜬다 — 끊긴 상태가 계속되는 동안 매번 다시
/// 덮으면 아무것도 할 수 없다.
///
/// 연결이 돌아오면 저절로 내려간다(``RunvealNetworkState``가 `NWPathMonitor`를 구독한다).
///
/// **Android와 다른 점.** Android는 `BackHandler`로 시스템 뒤로가기까지 가로채 닫는다. iOS에는
/// 시스템 뒤로가기가 없고 이 뷰가 화면 전체를 덮어 스와이프 제스처도 닿지 않으므로, 상단 앱바의
/// 화살표가 닫는 유일한 길이다.
struct NetworkErrorOverlay: View {
    @ObservedObject var state: RunvealNetworkState
    @State private var dismissed = false

    var body: some View {
        Group {
            if state.isConnected || dismissed {
                EmptyView()
            } else {
                NetworkErrorScreen(
                    // 연결이 돌아왔는지 다시 읽는다. 연결됐으면 isConnected가 true로 바뀌면서 이
                    // 화면이 내려간다 — 버튼이 직접 닫지 않는다.
                    onRetryClick: { state.refresh() },
                    onBackClick: { dismissed = true }
                )
            }
        }
        // 연결이 돌아오면 "닫음" 표시를 푼다 — 다음에 또 끊기면 다시 알려야 한다.
        .onChange(of: state.isConnected) { _, connected in
            if connected { dismissed = false }
        }
    }
}
