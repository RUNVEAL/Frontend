import FirebaseAnalytics
import FirebaseCore
import SwiftUI

@main
struct iOSApp: App {

    /// Firebase(Crashlytics·Analytics) 초기화 — 앱이 켜질 때 **가장 먼저 한 번**. 이걸 빠뜨리면
    /// 크래시도 지표도 수집되지 않는다. `GoogleService-Info.plist`는 `iosApp/iosApp/`에 있고, 그 폴더가 File System
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
        if Bundle.main.url(forResource: "GoogleService-Info", withExtension: "plist") != nil {
            FirebaseApp.configure()
            enableAnalyticsOnReleaseBuildsOnly()
        } else {
            print("[Firebase] GoogleService-Info.plist가 번들에 없어 초기화를 건너뛴다 — 크래시 리포트가 수집되지 않는다.")
        }
    }

    /// Analytics 수집은 **배포로 나가는 빌드에서만** 켠다 — 개발 중 기기에서 쏘는 이벤트가 지표에
    /// 섞이면 사용자 수·세션이 부풀어 보인다. Android도 같은 기준이다(buildType별 매니페스트 값).
    ///
    /// 기본값은 Info.plist의 `FIREBASE_ANALYTICS_COLLECTION_ENABLED=false`라, 이 함수가 켜 주지
    /// 않으면 꺼진 채로 남는다. 설정은 기기에 저장되므로 매 실행 호출해도 문제없다.
    private func enableAnalyticsOnReleaseBuildsOnly() {
        #if DEBUG
        print("[Firebase] 디버그 빌드 — Analytics 수집을 켜지 않는다.")
        #else
        Analytics.setAnalyticsCollectionEnabled(true)
        #endif
    }

    var body: some Scene {
        WindowGroup {
            RunvealRootView()
        }
    }
}
