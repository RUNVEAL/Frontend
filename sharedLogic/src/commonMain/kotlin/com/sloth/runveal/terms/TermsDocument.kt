package com.sloth.runveal.terms

/**
 * 약관 원문 주소. 본문을 앱에 넣지 않고 웹에 올려 둔 문서를 웹뷰로 띄운다 — 문구가 바뀌어도 앱을
 * 다시 배포하지 않고 갱신할 수 있다.
 *
 * **주소에 버전을 넣지 않는다.** 넣으면 약관을 고칠 때마다 이 파일도 같이 고쳐 배포해야 하고,
 * 그 사이에 구버전을 보여주게 된다. 대신 문서 안에 시행일을 적어 둔다.
 *
 * Android·iOS가 이 한 곳을 같이 읽는다 — 주소가 갈라지면 두 플랫폼 사용자가 서로 다른 약관에
 * 동의한 상태가 된다.
 */
object TermsDocument {
    /** 서비스 이용약관. */
    val serviceUrl: String = "https://runveal.github.io/terms/Runveal_Terms.html"

    /** 개인정보 처리방침. App Store Connect의 "개인정보 처리방침 URL"에도 같은 주소를 넣는다. */
    val privacyUrl: String = "https://runveal.github.io/terms/Runveal_Privacy.html"
}
