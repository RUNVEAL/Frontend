package com.sloth.runveal.ios

import com.sloth.runveal.map.LatLon
import com.sloth.runveal.map.snapshot.GeoBounds
import com.sloth.runveal.map.snapshot.SnapshotRequest
import com.sloth.runveal.map.snapshot.SnapshotVariant
import com.sloth.runveal.map.snapshot.toMultiPathSnapshotRequest
import com.sloth.runveal.map.snapshot.toSnapshotRequest
import com.sloth.runveal.map.snapshot.zoomToFit

/**
 * 지도 스냅샷 요청·줌 계산을 iOS에서 부르기 위한 얇은 래퍼.
 *
 * 둘 다 commonMain의 **최상위 확장 함수**인데(`List<LatLon>.toSnapshotRequest`,
 * `GeoBounds.zoomToFit`), Kotlin/Native는 최상위 선언을 `<파일이름>Kt` 클래스로 묶어서 내보내 Swift
 * 쪽 이름이 파일 이름에 묶인다. 파일을 옮기거나 쪼개면 Swift 호출부가 깨지므로, 이름이 안정적인
 * 객체 하나로 감싸 둔다.
 */
object IosSnapshot {

    /**
     * 점이 1개 이하인 경로는 스냅샷 요청을 만들지 않는다(null).
     *
     * [newRoadPaths]로 **러닝 기록인지 코스인지**가 갈린다:
     * - `null` — 아직 달리지 않은 코스. 경로를 한 색으로만 그린다.
     * - 목록(비어 있어도 됨) — 러닝 기록. 경로를 "오늘 달린 길"(흰색)로 깔고, 그중 처음 간 구간을
     *   "새로 탐험한 길"(금색)로 덧그린다.
     */
    fun request(
        points: List<LatLon>,
        ownerId: String,
        variant: SnapshotVariant,
        newRoadPaths: List<List<LatLon>>? = null,
    ): SnapshotRequest? = points.toSnapshotRequest(ownerId, variant, newRoadPaths)

    /**
     * 조각으로 나뉜 경로용 — 러닝 결과가 쓴다.
     *
     * 매칭 경로는 GPS와 동떨어진 부분을 잘라낸 **조각 목록**이라(`MatchedPathFilter`) 하나로 이어
     * 붙이면 잘라낸 구간이 직선으로 되살아난다. 점이 2개 이상인 조각이 하나도 없으면 null.
     */
    fun requestForPaths(
        paths: List<List<LatLon>>,
        ownerId: String,
        variant: SnapshotVariant,
        newRoadPaths: List<List<LatLon>>? = null,
    ): SnapshotRequest? = paths.toMultiPathSnapshotRequest(ownerId, variant, newRoadPaths)

    /** [bounds]를 (widthPx x heightPx) 뷰포트에 맞춰 보여줄 때 필요한 줌 레벨. */
    fun zoomToFit(bounds: GeoBounds, widthPx: Int, heightPx: Int): Double =
        bounds.zoomToFit(widthPx, heightPx)
}
