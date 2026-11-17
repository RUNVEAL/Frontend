package com.sloth.runveal.result

import com.sloth.runveal.map.LatLon

/**
 * "이 코스로 러닝 시작"으로 넘기는 코스. [isLoop]면 출발·도착이 같은 자리라 지도에 도착 핀을 따로
 * 띄우지 않는다 — 코스 상세/추천 결과 화면과 같은 규칙이다.
 */
data class SelectedCourse(val path: List<LatLon>, val isLoop: Boolean)

/**
 * "이 코스로 러닝 시작"으로 넘기는 코스 — `feature:course-recommend`/`feature:tab-course`와
 * `feature:running`이 서로 의존하지 않아 네비게이션 인자로 못 넘기니, [TrackResultHolder]와 같은
 * 패턴으로 sharedLogic에 둔다. 기록 화면은 이 값으로 점선 안내선과 출발·도착 핀을 그린다.
 */
object SelectedCourseHolder {
    private var selected: SelectedCourse? = null

    fun publish(path: List<LatLon>, isLoop: Boolean) {
        selected = SelectedCourse(path = path, isLoop = isLoop)
    }

    /** 한 번 읽으면 비운다 — 다음 기록 시작에 지난 코스가 그대로 남아있지 않게. */
    fun consume(): SelectedCourse? {
        val result = selected
        selected = null
        return result
    }
}
