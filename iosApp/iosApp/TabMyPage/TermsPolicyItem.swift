import Foundation
import SharedLogic

/**
 약관 및 정책 목록 항목 — Android `TermsPolicyItem`과 같은 구성.

 앱 버전은 눌러서 여는 문서가 아니라 값을 보여주는 줄이라 여기 넣지 않고 화면이 따로 그린다.

 ``url``이 있으면 그 주소를 웹뷰로 띄우고(``TermsDetailScreen``), 없으면 앱이 직접 그리는
 본문이다(오픈소스 라이선스).
 */
enum TermsPolicyItem: String, CaseIterable, Hashable {
    case service
    case privacy

    // 위치기반서비스 이용약관은 마이페이지 구성에서 빠졌다. 다시 넣기로 하면 이 줄만 되살리면 된다
    // — Android도 같은 자리에 주석으로 남겨 뒀다.
    // case location

    case openSource

    var label: String {
        switch self {
        case .service: return MyStrings.termsPolicyService
        case .privacy: return MyStrings.termsPolicyPrivacy
        case .openSource: return MyStrings.termsPolicyOpenSource
        }
    }

    /// 웹에 올려 둔 약관 원문. 주소는 Android와 같은 값을 쓰려고 sharedLogic에 둔다 — 갈라지면
    /// 두 플랫폼 사용자가 서로 다른 약관에 동의한 상태가 된다.
    var url: URL? {
        switch self {
        case .service: return URL(string: TermsDocument.shared.serviceUrl)
        case .privacy: return URL(string: TermsDocument.shared.privacyUrl)
        case .openSource: return nil
        }
    }
}
