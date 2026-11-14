package com.swyp.runveal.dong

/**
 * 코스의 "새 길"(아직 안 가본 길) 길이를 로컬에서 추정한다.
 *
 * 원래는 서버가 `POST /api/v1/routes` 응답의 `newRoadM`으로 내려주지만, 요청에
 * `exploredSegmentIds`를 못 보냈거나(`docs/api/API.md` §3.3.1 — 안 보내면 `newRoadM`이 null)
 * 서버가 값을 안 줬을 때 쓰는 대체 계산이다. `explored_segment`(가본 구간 캐시)에 코스가 지나는
 * 구간이 몇 m나 들어 있는지만 알면 되고, `road_segment` 인덱스가 아직 안 들여와 있어도 동작한다.
 *
 * 코스 전체 길이에서 이미 가본 구간 길이를 빼는 방식이라, 코스가 구간 일부만 지나가는 시작/끝
 * 지점에서는 그 구간 전체 길이가 빠져 새 길이 실제보다 조금 짧게 나올 수 있다 — 서버 값이 있으면
 * 항상 그쪽을 우선한다.
 */
object NewRoadEstimator {

    /**
     * [totalDistanceM]는 코스 전체 길이(m), [exploredLengthM]은 그 코스가 지나는 구간 중 이미 가본
     * 것의 길이 합(m). 반올림 오차나 위 근사 때문에 음수가 되거나 전체 길이를 넘지 않게 자른다.
     */
    fun newRoadMeters(totalDistanceM: Double, exploredLengthM: Double): Double =
        (totalDistanceM - exploredLengthM).coerceIn(0.0, totalDistanceM.coerceAtLeast(0.0))
}
