package com.swyp.runveal.tabmypage

import androidx.lifecycle.viewModelScope
import com.swyp.runveal.data.local.FileStore
import com.swyp.runveal.data.local.RunvealDatabaseProvider
import com.swyp.runveal.data.local.TrackLocalStore
import com.swyp.runveal.dong.DongExploreRate
import com.swyp.runveal.result.TrackResultHolder
import com.swyp.runveal.tabmypage.contract.MyIntent
import com.swyp.runveal.tabmypage.contract.MyState
import com.swyp.runveal.ui.mvi.MviViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 마이페이지. 위치 권한은 Compose 상태(`rememberLocationPermissionState`)를 화면이 직접 읽으므로
 * 여기로 오지 않는다 — 나머지 항목은 연결할 기능이 생기면 여기서 처리한다(각 intent의 TODO 참고).
 */
class MyViewModel : MviViewModel<MyIntent, MyState, Nothing>(
    initialState = MyState(),
) {
    private val trackLocalStore = TrackLocalStore(RunvealDatabaseProvider.database, FileStore())

    init {
        // 러닝이 끝나 매칭까지 완료되면 TrackResultHolder가 새 값을 내보낸다 — 그때마다 다시 집계해
        // 탭을 떠났다 돌아오지 않아도 숫자가 맞춰진다(HomeScreen이 "가본 길"을 갱신하는 방식과 같다).
        viewModelScope.launch {
            TrackResultHolder.state.collectLatest { loadRunningSummary() }
        }
    }

    override suspend fun handleIntent(intent: MyIntent) {
        when (intent) {
            // 아래 다섯은 아직 붙일 기능이 없어 전부 "준비 중" 안내만 띄운다. 각 TODO가 풀리는 대로
            // 하나씩 실제 동작으로 바꾼다.
            //  - TODO: 닉네임 변경 화면으로 이동 — 회원 정보 기능이 붙은 뒤.
            //  - TODO: Health Connect 기록 가져오기 — 가져오기 기능이 붙은 뒤.
            //  - TODO: 약관 및 정책 보기 — 약관 목록 API가 붙은 뒤.
            //  - TODO: 로그아웃 — auth가 붙은 뒤.
            //  - TODO: 회원 탈퇴 — 회원 API가 붙은 뒤(되돌릴 수 없으니 확인 다이얼로그 필요).
            MyIntent.NicknameChangeClicked,
            MyIntent.ImportRecordsClicked,
            MyIntent.TermsClicked,
            MyIntent.LogoutClicked,
            MyIntent.WithdrawClicked,
            -> setState { copy(showNotReadyToast = true) }

            MyIntent.ToastShown -> setState { copy(showNotReadyToast = false) }
        }
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
