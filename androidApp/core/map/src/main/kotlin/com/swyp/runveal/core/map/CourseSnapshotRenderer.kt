package com.swyp.runveal.core.map

import android.content.Context
import com.swyp.runveal.core.map.internal.snapshot.CourseSnapshotRendererImpl
import com.swyp.runveal.map.snapshot.SnapshotRequest
import java.io.File

/**
 * 코스/기록 썸네일, 러닝 결과 카드용 지도 스냅샷 이미지(`docs/running/COURSE_MAP_SNAPSHOT.md`).
 *
 * `THUMBNAIL_SIZE`/`RESULT_CARD_SIZE`를 고정 상수로 두지 않는다 — 코스 목록(72dp)과 기록 목록(80dp)의
 * 기존 썸네일 자리 크기가 이미 서로 달라서, 호출 쪽(Composable)이 실제로 그릴 픽셀 크기를
 * [widthPx]/[heightPx]로 직접 넘긴다. 캐시 파일명에도 이 크기가 들어가 같은 [SnapshotRequest]라도
 * 자리마다 별도로 캐시된다.
 */
interface CourseSnapshotRenderer {
    /** 캐시가 있으면 바로 반환하고, 없으면 렌더링해서 저장한 뒤 파일을 반환한다. 실패하면 `null`. */
    suspend fun getOrRender(request: SnapshotRequest, widthPx: Int, heightPx: Int): File?

    /** 렌더링 없이 캐시만 조회한다. */
    fun cachedFile(request: SnapshotRequest, widthPx: Int, heightPx: Int): File?

    /** 기록이나 코스가 삭제됐을 때 호출 — [ownerId]의 모든 variant/크기 캐시를 지운다. */
    fun delete(ownerId: String)
}

/** Compose 컨텍스트가 없는 곳(ViewModel 등)에서 [CourseSnapshotRenderer]를 만드는 공개 진입점 —
 * [CourseSnapshotImage]처럼 화면에 보일 때 그리는 대신, "코스 저장" 같은 시점에 미리 캐시를 만들어
 * 두고 싶을 때 쓴다. 색은 [CourseSnapshotImage]와 같은 이유로 `RunvealTheme.colors`에서 미리 뽑아 온
 * ARGB `Int`를 받는다. */
fun CourseSnapshotRenderer(
    context: Context,
    routeColor: Int,
    endpointColor: Int,
    endpointStrokeColor: Int,
): CourseSnapshotRenderer = CourseSnapshotRendererImpl(context, routeColor, endpointColor, endpointStrokeColor)
