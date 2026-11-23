package com.sloth.runveal.tabmypage

import androidx.lifecycle.viewModelScope
import com.sloth.runveal.auth.AuthProvider
import com.sloth.runveal.auth.usecase.GetNicknameUseCase
import com.sloth.runveal.auth.usecase.LogoutUseCase
import com.sloth.runveal.data.local.FileStore
import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.data.local.TrackLocalStore
import com.sloth.runveal.dong.DongExploreRate
import com.sloth.runveal.external.ImportProgress
import com.sloth.runveal.external.ImportProgressHolder
import com.sloth.runveal.result.TrackResultHolder
import com.sloth.runveal.tabmypage.contract.MyEffect
import com.sloth.runveal.tabmypage.contract.MyIntent
import com.sloth.runveal.tabmypage.contract.MyState
import com.sloth.runveal.tabmypage.contract.MyToast
import com.sloth.runveal.ui.mvi.MviViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 마이페이지. 위치 권한은 Compose 상태(`rememberLocationPermissionState`)를 화면이 직접 읽으므로
 * 여기로 오지 않는다 — 나머지 항목은 연결할 기능이 생기면 여기서 처리한다(각 intent의 TODO 참고).
 */
class MyViewModel : MviViewModel<MyIntent, MyState, MyEffect>(
    initialState = MyState(),
) {
    private val trackLocalStore = TrackLocalStore(RunvealDatabaseProvider.database, FileStore())
    private val getNickname = GetNicknameUseCase(AuthProvider.repository)
    private val logout = LogoutUseCase(AuthProvider.repository)

    init {
        viewModelScope.launch { loadNickname() }

        // 러닝이 끝나 매칭까지 완료되면 TrackResultHolder가 새 값을 내보낸다 — 그때마다 다시 집계해
        // 탭을 떠났다 돌아오지 않아도 숫자가 맞춰진다(HomeScreen이 "가본 길"을 갱신하는 방식과 같다).
        viewModelScope.launch {
            TrackResultHolder.state.collectLatest { loadRunningSummary() }
        }

        // Health Connect에서 기록을 가져오면 "탐험한 동"·"누적 거리"가 늘어난다. 진행 화면은 이
        // 화면 위에 올라갔다 내려오기만 해서 ViewModel이 다시 만들어지지 않으므로, 끝나는 걸 보고
        // 직접 다시 집계한다.
        viewModelScope.launch {
            ImportProgressHolder.state.collectLatest { progress ->
                if (progress is ImportProgress.Finished) loadRunningSummary()
            }
        }
    }

    override suspend fun handleIntent(intent: MyIntent) {
        when (intent) {
            // 아래 둘은 아직 붙일 기능이 없어 "준비 중" 안내만 띄운다. 각 TODO가 풀리는 대로
            // 하나씩 실제 동작으로 바꾼다.
            //  - TODO: 닉네임 변경 화면으로 이동 — 회원 정보 기능이 붙은 뒤.
            //  - TODO: 회원 탈퇴 — 회원 API가 붙은 뒤(되돌릴 수 없으니 확인 다이얼로그 필요).
            MyIntent.NicknameChangeClicked,
            MyIntent.WithdrawClicked,
            -> setState { copy(toast = MyToast.NOT_READY) }

            // 로그아웃은 되돌리려면 다시 로그인해야 해서 한 번 더 묻는다.
            MyIntent.LogoutClicked -> setState { copy(showLogoutDialog = true) }
            MyIntent.LogoutDismissed -> setState { copy(showLogoutDialog = false) }
            MyIntent.LogoutConfirmed -> logout()

            // ⚠️ 테스트용 — 기록이 늘었으니 "탐험한 동"·"누적 거리"를 다시 센다(Health Connect
            // 가져오기가 끝났을 때와 같은 처리).
            is MyIntent.SampleGpxImported -> {
                loadRunningSummary()
                setState {
                    copy(
                        toast = when {
                            intent.imported > 0 -> MyToast.SAMPLE_GPX_IMPORTED
                            intent.skipped > 0 -> MyToast.SAMPLE_GPX_ALREADY
                            else -> MyToast.SAMPLE_GPX_FAILED
                        },
                    )
                }
            }

            is MyIntent.ToastRequested -> setState { copy(toast = intent.toast) }
            MyIntent.ToastShown -> setState { copy(toast = null) }
        }
    }

    /**
     * 로그아웃. [com.sloth.runveal.auth.repository.AuthRepository.logout]이 소셜 SDK 로그아웃
     * (카카오 `logout`, 구글 `clearCredentialState`)과 로컬 토큰 삭제를 함께 처리한다. 둘 중 하나가
     * 실패해도 나머지는 진행되고(각각 runCatching), 어느 쪽이든 로그인 화면으로 돌려보낸다 —
     * 로컬 토큰이 남은 채 머물면 로그아웃한 것처럼 보이는데 실제로는 로그인 상태가 된다.
     */
    private suspend fun logout() {
        if (currentState.isLoggingOut) return
        setState { copy(showLogoutDialog = false, isLoggingOut = true) }

        withContext(Dispatchers.IO) { runCatching { logout.invoke() } }
        // 스플래시·홈이 보는 전역 상태를 바로 확정한다 — 이동 후에 바꾸면 그 사이 프레임에서
        // 아직 로그인 상태로 보인다.
        AuthProvider.session.markLoggedOut()

        setState { copy(isLoggingOut = false) }
        sendEffect(MyEffect.LoggedOut)
    }

    /**
     * 가입 때 정한 닉네임. 아직 가입을 안 끝냈으면 null이라 상태는 빈 문자열로 두고, 화면이
     * 자리표시자를 대신 보여준다.
     */
    private suspend fun loadNickname() = withContext(Dispatchers.IO) {
        val nickname = runCatching { getNickname() }.getOrNull().orEmpty()
        setState { copy(nickname = nickname) }
    }

    /**
     * "탐험한 동"은 지도가 빛나는 기준과 같은 [DongExploreRate.percents]를 쓴다 — 탐험률이 0%보다
     * 큰 동의 개수다. 여기서 따로 세면 지도에는 3개가 빛나는데 마이페이지엔 5개라고 적히는 식으로
     * 어긋난다. 구간 인덱스를 아직 안 들여온 동(분모 없음)은 양쪽 모두에서 빠진다.
     */
    private suspend fun loadRunningSummary() = withContext(Dispatchers.IO) {
        val exploredDongCount = DongExploreRate.percents(
            walkedLengthsByDong = trackLocalStore.walkedLengthByDong(),
            totalLengthsByDong = trackLocalStore.totalLengthByDong(),
        ).size
        val totalDistanceKm = trackLocalStore.totalDistanceMeters() / 1000.0

        setState { copy(exploredDongCount = exploredDongCount, totalDistanceKm = totalDistanceKm) }
    }
}
