package com.swyp.runveal.tabmypage.contract

import com.swyp.runveal.feature.tabmypage.R

/**
 * 약관 및 정책 목록 항목. 앱 버전은 눌러서 여는 문서가 아니라 값을 보여주는 줄이라 여기 넣지 않고
 * 화면이 따로 그린다.
 */
enum class TermsPolicyItem(val labelRes: Int) {
    Service(R.string.terms_policy_service),
    Privacy(R.string.terms_policy_privacy),

    // TODO: 위치기반서비스 이용약관(terms_policy_location)은 시안에서 빠져 있어 뺐다. 위치를
    //  수집하는 앱이라 필요한 문서일 수 있으니, 넣기로 하면 이 줄만 되살리면 된다.
    //  Location(R.string.terms_policy_location),

    OpenSource(R.string.terms_policy_open_source),
}
