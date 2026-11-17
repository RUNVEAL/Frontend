package com.sloth.runveal.sign.signup

/**
 * 로그인에서 받은 소셜 계정 이름을 닉네임 화면의 입력 기본값으로 넘긴다. 로그인 → 약관 동의 →
 * 닉네임으로 화면이 하나 더 끼어 있어서 네비게이션 인자로 넘기려면 약관 화면까지 인자를 들고
 * 가야 하는데, 닉네임에는 공백·특수문자가 섞일 수 있어 라우트에 실어 보내기에도 적절치 않다.
 * 그래서 `CourseRecommendResultHolder`와 같은 홀더 방식으로 전달한다.
 *
 * 프로세스가 죽으면 값이 사라지지만, 그때는 스플래시가 로그인부터 다시 받으므로 여기도 다시 채워진다.
 */
object SuggestedNicknameHolder {
    /** 소셜 계정 이름. 못 받았으면 null — 그러면 닉네임 입력은 공란으로 시작한다. */
    var value: String? = null
        private set

    /** 로그인할 때마다 덮어쓴다 — 다른 계정으로 다시 로그인했을 때 옛 이름이 남으면 안 된다. */
    fun publish(displayName: String?) {
        value = displayName
    }
}
