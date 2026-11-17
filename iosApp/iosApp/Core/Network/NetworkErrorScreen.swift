import SwiftUI

/// 네트워크 오류 화면의 정적 텍스트 — Android `core:ui`의 `res/values/strings.xml`과 1:1 대응.
enum NetworkErrorStrings {
    static let title = "인터넷 연결을 확인해 주세요"
    static let description = "연결 상태를 확인한 뒤 다시 시도해 주세요"
    static let retry = "다시 시도"
}

/// 와이파이·모바일 데이터가 없어 아무 요청도 보낼 수 없을 때 내용 대신 보여주는 화면
/// (디자인 `Z1-1 네트워크 오류`). 레이아웃은 전부 ``RunvealEmptyState``가 맡는다 — 시안의
/// `List/Empty State`가 그 컴포넌트와 같은 구성(80pt 그래픽 → 제목 + 설명 → Gold M 버튼)이다.
///
/// 연결 여부는 이 화면이 직접 보지 않는다 — 띄울지 말지는 ``RunvealNetworkState``를 구독하는
/// 호출부가 정한다. 연결이 돌아오면 그 상태가 바뀌면서 화면이 알아서 내려간다.
struct NetworkErrorScreen: View {
    /// "다시 시도" — 연결이 돌아왔을 때 다시 해야 할 일은 화면마다 다르므로(목록 다시 불러오기,
    /// 매칭 재요청 등) 호출부가 정한다. 연결 자체는 버튼으로 복구할 수 없다.
    let onRetryClick: () -> Void

    /// 뒤로가기. nil이면 상단 앱바를 그리지 않는다 — 탭 안쪽처럼 돌아갈 곳이 없는 자리에 그대로
    /// 끼워 넣을 때 쓴다.
    var onBackClick: (() -> Void)?

    var body: some View {
        ZStack {
            RunvealTheme.colors.bg.canvas.ignoresSafeArea()

            if let onBackClick {
                // 앱바는 세이프 에어리어를 침범하지 않는다 — Android가 statusBars 인셋을 주는 것과 같다.
                VStack(spacing: 0) {
                    RunvealTopAppBar(leading: .back, onLeadingClick: onBackClick)
                    Spacer(minLength: 0)
                }
            }

            RunvealEmptyState(
                title: NetworkErrorStrings.title,
                description: NetworkErrorStrings.description,
                actionLabel: NetworkErrorStrings.retry,
                onActionClick: onRetryClick,
                graphic: {
                    // design/images/error.png — 자체 색을 가진 그래픽이라 틴트를 입히지 않는다.
                    // 크기(80pt)는 RunvealEmptyState가 잡아 준다.
                    RunvealImages.error
                        .resizable()
                        .scaledToFit()
                }
            )
            .padding(.horizontal, RunvealLayout.marginScreen)
        }
    }
}

#Preview {
    NetworkErrorScreen(onRetryClick: {}, onBackClick: {})
}
