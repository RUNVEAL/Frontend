package com.swyp.runveal.tabmypage

import com.swyp.runveal.tabmypage.contract.MyIntent
import com.swyp.runveal.tabmypage.contract.MyState
import com.swyp.runveal.ui.mvi.MviViewModel

/**
 * 마이페이지. 위치 권한은 Compose 상태(`rememberLocationPermissionState`)를 화면이 직접 읽으므로
 * 여기로 오지 않는다 — 나머지 항목은 연결할 기능이 생기면 여기서 처리한다(각 intent의 TODO 참고).
 */
class MyViewModel : MviViewModel<MyIntent, MyState, Nothing>(
    initialState = MyState(),
) {
    override suspend fun handleIntent(intent: MyIntent) {
        when (intent) {
            // TODO: 닉네임 변경 화면으로 이동 — 회원 정보 기능이 붙은 뒤 연결한다.
            MyIntent.NicknameChangeClicked -> Unit

            // TODO: Health Connect 기록 가져오기 실행 — 가져오기 기능이 붙은 뒤 연결한다.
            MyIntent.ImportRecordsClicked -> Unit

            // TODO: 약관 및 정책 보기 — 약관 목록 API가 붙은 뒤 연결한다.
            MyIntent.TermsClicked -> Unit

            // TODO: 로그아웃 — auth가 붙은 뒤 연결한다.
            MyIntent.LogoutClicked -> Unit

            // TODO: 회원 탈퇴 — 회원 API가 붙은 뒤 연결한다(되돌릴 수 없으니 확인 다이얼로그 필요).
            MyIntent.WithdrawClicked -> Unit
        }
    }
}
