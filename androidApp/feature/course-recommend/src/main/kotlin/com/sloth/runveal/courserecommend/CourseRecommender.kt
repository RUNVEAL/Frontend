package com.sloth.runveal.courserecommend

import android.util.Log
import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.dong.SegmentCoverageMatcher
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.network.meta.MetaApi
import com.sloth.runveal.network.meta.MetaHolder
import com.sloth.runveal.network.routes.LatLng
import com.sloth.runveal.network.routes.RoutesApi
import com.sloth.runveal.network.routes.RoutesRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "CourseRecommendDebug"

/** 출발지를 아직 안 골랐고 현재 위치도 못 받았을 때 쓰는 최후 기본 좌표(성수동 인근). */
private val TEMP_START_LOCATION = LatLon(lat = 37.5445, lon = 127.0557)

private fun LatLon.toRoutesLatLng(): LatLng = LatLng(lat = lat, lng = lon)

/**
 * `docs/api/API.md` §3.3 — 조건대로 `/api/v1/routes`를 부른다. 코스 만들기(B0)와 추천 결과 화면
 * (B1, 필터를 바꿔 다시 받기)이 **같은 경로**로 요청하도록 한곳에 모아 뒀다.
 *
 * `selectedFilters`는 화면 라벨(한글)이라 서버 필터 코드(`PARK`/`RIVERSIDE`/`GENTLE_SLOPE`)와 1:1로
 * 안 맞아서 아직 요청에 싣지 않는다 — 칩 표시와 "조건을 바꿨다"는 신호로만 쓴다.
 */
internal class CourseRecommender {
    private val routesApi = RoutesApi()
    private val database = RunvealDatabaseProvider.database

    /**
     * 성공·실패를 [CourseRecommendResult]로 감싸 돌려준다 — 던지지 않으므로 호출부는 그대로
     * [CourseRecommendResultHolder]에 publish하면 된다.
     *
     * [fallbackStart]는 출발지를 안 골랐을 때 쓸 현재 위치(화면이 미리 받아 둔 값).
     */
    suspend fun recommend(settings: CourseRecommendSettings, fallbackStart: LatLon?): CourseRecommendResult = try {
        val meta = MetaHolder.state.value ?: MetaApi().fetchMeta().also { MetaHolder.publish(it) }
        val response = routesApi.fetchRoutes(
            RoutesRequest(
                mapVersion = meta.mapVersions.current,
                start = (settings.startLatLon ?: fallbackStart ?: TEMP_START_LOCATION).toRoutesLatLng(),
                // 도착지를 안 골랐으면 null — 자동으로 순환(LOOP) 코스가 된다(§3.3.1).
                end = settings.endLatLon?.toRoutesLatLng(),
                distanceM = parseDistanceMeters(settings.distanceLabel),
                preferUnexplored = settings.avoidVisitedPreferred,
                exploredSegmentIds = loadExploredSegmentIds(meta.limits.routes.maxExploredSegmentIds),
            ),
        )
        Log.i(TAG, "추천 성공 routes=${response.routes.size} warnings=${response.warnings}")
        CourseRecommendResult.Success(response, settings)
    } catch (e: Exception) {
        Log.w(TAG, "추천 실패", e)
        CourseRecommendResult.Failure(e.message ?: "코스를 추천받지 못했어요", settings)
    }

    /** 이미 가본 구간 id — 서버가 "새 길" 비중을 계산하는 데 쓴다. 상한을 넘으면 잘라서 보낸다. */
    private suspend fun loadExploredSegmentIds(maxCount: Int): List<Long> = withContext(Dispatchers.IO) {
        val all = database.exploredSegmentQueries
            .selectCoveredSegmentIds(SegmentCoverageMatcher.DEFAULT_SEGMENT_COVERAGE)
            .executeAsList()
        if (all.size > maxCount) Log.w(TAG, "가본 구간 ${all.size}개 > 상한 ${maxCount}개 — 잘라서 보낸다")
        all.take(maxCount)
    }

    /** "5km"/"12.5km" → 5000/12500. 프리셋·커스텀 값 전부 0.5km 단위라 서버의 500m 단위 제약과 맞는다. */
    private fun parseDistanceMeters(distanceLabel: String): Int =
        (distanceLabel.removeSuffix("km").toDouble() * 1000).toInt()
}
