package com.swyp.runveal.result

import com.swyp.runveal.map.LatLon

/**
 * "이 코스로 러닝 시작"으로 넘기는 코스 경로 — `feature:course-recommend`와 `feature:running`이 서로
 * 의존하지 않아 네비게이션 인자로 못 넘기니, [TrackResultHolder]와 같은 패턴으로 sharedLogic에 둔다.
 * 지금은 기록 화면에 전달만 하고(보관), 실제 가이드/오버레이 적용은 보류.
 */
object SelectedCourseHolder {
    private var path: List<LatLon>? = null

    fun publish(path: List<LatLon>) {
        this.path = path
    }

    /** 한 번 읽으면 비운다 — 다음 기록 시작에 지난 코스가 그대로 남아있지 않게. */
    fun consume(): List<LatLon>? {
        val result = path
        path = null
        return result
    }
}
