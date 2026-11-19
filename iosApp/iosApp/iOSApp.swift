import FirebaseCore
import SwiftUI

@main
struct iOSApp: App {

    /// Firebase(Crashlytics) 초기화 — 앱이 켜질 때 **가장 먼저 한 번**. 이걸 빠뜨리면 크래시가
    /// 수집되지 않는다. `GoogleService-Info.plist`는 `iosApp/iosApp/`에 있고, 그 폴더가 File System
    /// Synchronized Group이라 따로 등록하지 않아도 번들에 들어간다 — 들어갔는지는 CI의
    /// "아카이브 번들 점검" 단계가 매 빌드마다 찍어 준다.
    ///
    /// **설정 파일이 없으면 초기화를 건너뛴다.** `FirebaseApp.configure()`는 번들에서 plist를 못
    /// 찾으면 `fatalError`로 앱을 죽인다 — 크래시를 보려고 넣은 것이 앱을 아예 못 뜨게 만들고,
    /// 그 시점엔 Crashlytics가 시작 전이라 리포트도 남지 않아 원인조차 알 수 없다. 진단 수단이
    /// 본체를 망가뜨리지 않게 존재 확인을 먼저 한다.
    ///
    /// Crashlytics는 튕긴 그 순간이 아니라 **다음 실행 때** 지난 크래시를 올린다 — 튕긴 뒤 앱을
    /// 한 번 더 켜야 콘솔에 보인다.
    init() {
        // 지난 실행이 남긴 진단 기록을 넘겨받고 파일을 비운다 — Firebase보다 먼저 해야 한다.
        RunvealTrace.startNewSession()

        if Bundle.main.url(forResource: "GoogleService-Info", withExtension: "plist") != nil {
            FirebaseApp.configure()
        } else {
            print("[Firebase] GoogleService-Info.plist가 번들에 없어 초기화를 건너뛴다 — 크래시 리포트가 수집되지 않는다.")
        }
    }

    var body: some Scene {
        WindowGroup {
            RunvealRootView()
        }
    }
}
