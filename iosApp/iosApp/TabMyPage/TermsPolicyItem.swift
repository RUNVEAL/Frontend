import Foundation

/**
 약관 및 정책 목록 항목 — Android `TermsPolicyItem`과 같은 구성.

 앱 버전은 눌러서 여는 문서가 아니라 값을 보여주는 줄이라 여기 넣지 않고 화면이 따로 그린다.
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
}
