package com.sloth.runveal.ios

import com.sloth.runveal.data.local.FileStore
import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.data.local.TrackLocalStore
import com.sloth.runveal.dong.DongExploreRate

/** MY 탭 프로필 카드에 띄우는 누적 수치. */
data class MyRunningSummary(
    /** 탐험한 동 개수 — 1% 이상 밟은 동만 센다. */
    val exploredDongCount: Int,
    /** 누적 거리(km), 소수점 둘째 자리까지 — 시안의 "35.10" 표기. */
    val totalDistanceLabel: String,
)

/**
 * MY 탭이 쓰는 집계 — Android `MyViewModel.loadRunningSummary()`와 같은 계산이다.
 *
 * 화면이 DB를 직접 건드리지 않게 여기서 묶는다. Swift는 결과만 받아 그린다.
 */
object IosMyPageData {

    private val store by lazy { TrackLocalStore(RunvealDatabaseProvider.database, FileStore()) }

    /**
     * 탐험한 동 수는 **지도 빛·홈 요약 카드와 같은 집계**([DongExploreRate.percents])로 센다 —
     * 다른 식으로 세면 "탐험한 동 3개"인데 지도에는 두 곳만 빛나는 상태가 된다. 구간 인덱스를
     * 아직 안 들여온 동(분모 없음)은 양쪽 모두에서 빠진다.
     */
    fun loadRunningSummary(): MyRunningSummary {
        val exploredDongCount = DongExploreRate.percents(
            walkedLengthsByDong = store.walkedLengthByDong(),
            totalLengthsByDong = store.totalLengthByDong(),
        ).size

        return MyRunningSummary(
            exploredDongCount = exploredDongCount,
            totalDistanceLabel = formatKilometers(store.totalDistanceMeters() / 1000.0),
        )
    }

    /**
     * 소수점 둘째 자리까지, 버림. Android `MyState.totalDistanceLabel`과 같은 규칙이라 두 플랫폼
     * 숫자가 어긋나지 않는다 — iOS의 `String(format:)`은 반올림이라 쓰지 않는다.
     */
    private fun formatKilometers(km: Double): String {
        val rounded = (km * 100).toLong()
        return "${rounded / 100}.${(rounded % 100).toString().padStart(2, '0')}"
    }
}
