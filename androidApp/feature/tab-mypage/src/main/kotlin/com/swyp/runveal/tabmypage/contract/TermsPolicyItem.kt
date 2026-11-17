package com.swyp.runveal.tabmypage.contract

import com.swyp.runveal.feature.tabmypage.R

/**
 * 약관 및 정책 목록 항목. 앱 버전은 눌러서 여는 문서가 아니라 값을 보여주는 줄이라 여기 넣지 않고
 * 화면이 따로 그린다.
 */
enum class TermsPolicyItem(val labelRes: Int) {
    Service(R.string.terms_policy_service),
    Privacy(R.string.terms_policy_privacy),
    Location(R.string.terms_policy_location),
    OpenSource(R.string.terms_policy_open_source),
}
