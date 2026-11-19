import FirebaseCore
import SwiftUI

@main
struct iOSApp: App {

    /// Firebase(Crashlytics) 초기화 — 앱이 켜질 때 **가장 먼저 한 번**. 이걸 빠뜨리면 크래시가
    /// 수집되지 않는다. `GoogleService-Info.plist`는 `iosApp/iosApp/`에 있고, 그 폴더가 File System
    /// Synchronized Group이라 따로 등록하지 않아도 번들에 들어간다.
    ///
    /// Crashlytics는 튕긴 그 순간이 아니라 **다음 실행 때** 지난 크래시를 올린다 — 튕긴 뒤 앱을
    /// 한 번 더 켜야 콘솔에 보인다.
    init() {
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            RunvealRootView()
        }
    }
}
