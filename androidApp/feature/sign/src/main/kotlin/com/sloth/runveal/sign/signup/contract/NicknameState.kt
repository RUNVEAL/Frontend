package com.sloth.runveal.sign.signup.contract

import com.sloth.runveal.ui.mvi.UiState

internal const val NICKNAME_MIN_LENGTH = 2
internal const val NICKNAME_MAX_LENGTH = 10

/** 한글(완성형·자모)·영문·숫자만 허용. 공백과 특수문자는 막는다. */
internal fun Char.isAllowedNicknameChar(): Boolean =
    this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9' || this in '가'..'힣' || this in 'ㄱ'..'ㆎ'

/**
 * TODO(서버 인증) 실기기에서 이 값이 비어 들어오는 것을 확인했다 — 원인은
 * [com.sloth.runveal.auth.service.KakaoAuthService]의 같은 태그 주석 참고. 비면 공란으로 시작할 뿐
 * 가입은 그대로 되므로 막는 문제는 아니다.
 *
 * 소셜 계정 이름을 닉네임 입력 기본값으로 쓸 수 있게 다듬는다. 카카오·구글 쪽 이름에는 공백이나
 * 특수문자가 섞여 있을 수 있어서(예: "김 도윤", "Doyun Kim!") 그대로 넣으면 화면에 들어서자마자
 * 오류 문구가 뜬다. 허용 문자만 남기고 최대 길이로 자른 뒤, 그래도 최소 길이를 못 채우면 공란으로 둔다.
 */
internal fun suggestedNicknameOrEmpty(displayName: String?): String {
    val filtered = displayName.orEmpty().filter { it.isAllowedNicknameChar() }.take(NICKNAME_MAX_LENGTH)
    return if (filtered.length >= NICKNAME_MIN_LENGTH) filtered else ""
}

data class NicknameState(
    val nickname: String = "",
    /** 가입 확정 요청 중. 그 사이 버튼을 다시 못 누르게 막는다. */
    val isSubmitting: Boolean = false,
    val showError: Boolean = false,
) : UiState {
    val hasInvalidChar: Boolean get() = nickname.any { !it.isAllowedNicknameChar() }

    val isValid: Boolean
        get() = nickname.length in NICKNAME_MIN_LENGTH..NICKNAME_MAX_LENGTH && !hasInvalidChar

    val canSubmit: Boolean get() = isValid && !isSubmitting
}
