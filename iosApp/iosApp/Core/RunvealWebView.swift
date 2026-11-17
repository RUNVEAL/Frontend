import SwiftUI
import WebKit

/**
 원격 문서 한 건을 띄우는 웹뷰 — 지금은 약관이 쓴다(`TermsDetailScreen`).

 자바스크립트는 끈다. 약관은 정적 HTML이라 필요 없고, 꺼 두는 쪽이 안전하다.

 로드가 끝나기 전까지 캔버스 색으로 덮인 채 로딩 모션만 보인다. 배경을 칠하지 않으면 로드 직전에
 흰 화면이 한 번 번쩍인다 — 앱이 다크 단일 테마라 특히 눈에 띈다.

 원문 HTML이 이미 다크 테마라 앱 안에서 겉돌지 않는다(Android 실기기 확인). 원문을 밝은 배경으로
 바꾸면 이 화면만 희게 뜨므로, 그때는 원문에 `prefers-color-scheme`을 넣는 쪽이 낫다.
 */
struct RunvealWebView: View {
    let url: URL

    @State private var isLoading = true

    var body: some View {
        ZStack {
            RunvealTheme.colors.bg.canvas

            WebViewRepresentable(url: url, isLoading: $isLoading)

            if isLoading {
                RunvealLoading()
            }
        }
    }
}

private struct WebViewRepresentable: UIViewRepresentable {
    let url: URL
    @Binding var isLoading: Bool

    func makeCoordinator() -> Coordinator {
        Coordinator(isLoading: $isLoading)
    }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        // 약관은 정적 HTML이라 JS가 필요 없다.
        configuration.defaultWebpagePreferences.allowsContentJavaScript = false

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = context.coordinator
        // 로드 전 흰 배경이 비치지 않게 — 스크롤 바운스 영역도 같이 덮는다.
        webView.isOpaque = false
        webView.backgroundColor = .clear
        webView.scrollView.backgroundColor = .clear
        webView.load(URLRequest(url: url))
        return webView
    }

    /// 주소는 약관 한 건마다 고정이라 다시 로드할 일이 없다 — 여기서 `load`를 또 부르면 SwiftUI가
    /// 뷰를 갱신할 때마다 페이지가 처음부터 다시 뜬다.
    func updateUIView(_ webView: WKWebView, context: Context) {}

    final class Coordinator: NSObject, WKNavigationDelegate {
        @Binding private var isLoading: Bool

        init(isLoading: Binding<Bool>) {
            _isLoading = isLoading
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            isLoading = false
        }

        /// 실패해도 로딩 모션은 걷는다 — 안 그러면 영원히 도는 화면이 된다. 빈 화면이 남지만
        /// 네트워크가 끊긴 경우라면 `NetworkErrorOverlay`가 그 위를 덮는다.
        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            isLoading = false
        }

        func webView(
            _ webView: WKWebView,
            didFailProvisionalNavigation navigation: WKNavigation!,
            withError error: Error
        ) {
            isLoading = false
        }
    }
}
