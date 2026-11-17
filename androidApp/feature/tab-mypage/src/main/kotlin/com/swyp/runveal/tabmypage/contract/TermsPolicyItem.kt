package com.swyp.runveal.tabmypage.contract

import com.swyp.runveal.feature.tabmypage.R
import com.swyp.runveal.terms.TermsDocument

/**
 * 약관 및 정책 목록 항목. 앱 버전은 눌러서 여는 문서가 아니라 값을 보여주는 줄이라 여기 넣지 않고
 * 화면이 따로 그린다.
 *
 * [url]이 있으면 그 주소를 웹뷰로 띄우고([TermsDetailScreen]), 없으면 앱이 직접 그리는 본문이다
 * (오픈소스 라이선스).
 */
enum class TermsPolicyItem(val labelRes: Int, val url: String? = null) {
    Service(R.string.terms_policy_service, TermsDocument.serviceUrl),
    Privacy(R.string.terms_policy_privacy, TermsDocument.privacyUrl),

    // 위치기반서비스 이용약관(terms_policy_location)은 마이페이지 구성에서 빠졌다. 다시 넣기로
    // 하면 이 줄만 되살리면 된다 — 문자열은 그대로 두었다.
    // Location(R.string.terms_policy_location),

    OpenSource(R.string.terms_policy_open_source),
}
