package com.sloth.runveal.recording

import com.sloth.runveal.map.GeoMath
import com.sloth.runveal.map.LatLon

/** [TrackFilter]가 점을 버린 이유 — `docs/running/recording-lifecycle.md` §4.1 표. */
enum class DropReason { ACCURACY, SPEED, DISTANCE }

/**
 * raw GPS 점을 받아 filtered 목록에 넣을지 판정한다(`docs/running/recording-lifecycle.md` §4.1).
 * **직전 유지 점**(filtered 리스트의 마지막 점) 기준으로 판정한다 — 직전 raw 점이 아니다, raw 점은
 * 노이즈가 많아서 그걸 기준으로 하면 필터가 자기 자신을 계속 깬다.
 *
 * 기준값(정확도 20m/속도 8m/s/거리 3m)은 레거시 프로토타입 값 그대로다 — 실제 러닝 데이터로 조정
 * 전까지는(§9-2) 바뀌지 않는다. GPS 요청 간격(5초, `TrackingService` 쪽 설정)과는 별개 — 이 필터는
 * "받은 점을 쓸지 말지"만 판정하고, "얼마나 자주 받을지"는 안 건드린다.
 */
object TrackFilter {
    private const val MAX_ACCURACY_METERS = 20f
    private const val MAX_SPEED_MPS = 8f // ~29km/h
    private const val MIN_DISTANCE_METERS = 3.0

    /** `null`이면 통과(filtered에 추가), 아니면 버려진 이유. */
    fun evaluate(point: TrackPoint, lastKept: TrackPoint?): DropReason? {
        if (point.accuracyMeters > MAX_ACCURACY_METERS) return DropReason.ACCURACY
        if (point.speedMps > MAX_SPEED_MPS) return DropReason.SPEED
        if (lastKept != null) {
            val distance = GeoMath.distanceMeters(LatLon(lastKept.lat, lastKept.lon), LatLon(point.lat, point.lon))
            if (distance < MIN_DISTANCE_METERS) return DropReason.DISTANCE
        }
        return null
    }
}
