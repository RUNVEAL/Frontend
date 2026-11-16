package com.swyp.runveal.sign.signup.contract

import com.swyp.runveal.ui.mvi.UiState

internal const val NICKNAME_MIN_LENGTH = 2
internal const val NICKNAME_MAX_LENGTH = 10

/** 한글(완성형·자모)·영문·숫자만 허용. 공백과 특수문자는 막는다. */
private fun Char.isAllowedNicknameChar(): Boolean =
    this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9' || this in '가'..'힣' || this in 'ㄱ'..'ㆎ'

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
