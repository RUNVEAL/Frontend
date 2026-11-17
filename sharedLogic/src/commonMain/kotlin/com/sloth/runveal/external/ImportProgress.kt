package com.sloth.runveal.external

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 외부 기록 가져오기 진행 상황. UI는 아직 없고([ImportProgressHolder]를 구독할 화면은 추후 작업),
 * 지금은 가져오기 로직이 상태를 내보내는 쪽만 맞춰 둔다.
 *
 * `IMPORT`는 서버 전체에서 동시에 1건만 처리되므로(§3.7) 한 건씩 순서대로 진행한다 — 그래서
 * 진행률이 "n / total" 형태로 의미가 있다.
 */
sealed interface ImportProgress {
    data object Idle : ImportProgress

    /** 가져올 세션 목록을 훑는 중. 아직 총 개수를 모른다. */
    data object Scanning : ImportProgress

    /**
     * [done]건 끝냈고 [total]건이 대상이다. [failed]는 매칭 실패·경로 없음 등으로 건너뛴 건수 —
     * 실패해도 전체를 멈추지 않고 다음 건으로 넘어간다.
     */
    data class Running(val done: Int, val total: Int, val failed: Int) : ImportProgress

    data class Finished(val imported: Int, val skipped: Int, val failed: Int) : ImportProgress

    /** 가져오기 자체를 시작조차 못 한 경우(권한 없음, Health Connect 미설치 등). */
    data class Unavailable(val reason: ImportUnavailableReason) : ImportProgress
}

enum class ImportUnavailableReason {
    /** 기기에 Health Connect가 없거나 업데이트가 필요하다. */
    PROVIDER_NOT_AVAILABLE,

    /** 운동 기록 읽기 권한이 없다. */
    PERMISSION_DENIED,

    /** 경로(GPS) 읽기 권한이 없어 도로 매칭을 할 수 없다. */
    ROUTE_PERMISSION_DENIED,
}

object ImportProgressHolder {
    private val _state = MutableStateFlow<ImportProgress>(ImportProgress.Idle)
    val state: StateFlow<ImportProgress> = _state.asStateFlow()

    fun publish(progress: ImportProgress) {
        _state.value = progress
    }
}
