package com.swyp.runveal.ios

import com.swyp.runveal.data.local.FileStore
import com.swyp.runveal.data.local.RunvealDatabaseProvider
import com.swyp.runveal.data.local.TrackLocalStore
import com.swyp.runveal.result.LiveMatchHolder
import com.swyp.runveal.result.LiveNewRoadHolder

/**
 * 홈 지도가 그리는 선 레이어 데이터 — Android `HomeScreen.HomeMapLayer`의 DB/분류 부분에 해당한다.
 * 화면은 여기서 받은 GeoJSON 문자열을 지도 소스에 그대로 넣기만 한다.
 */
object IosHomeMapData {

    private val trackLocalStore by lazy {
        TrackLocalStore(RunvealDatabaseProvider.database, FileStore())
    }

    private val liveRoadClassifier by lazy { IosLiveRoadClassifier() }

    /** 지금까지 쌓인 "가본 길" 선 — 러닝 기록이 추가될 때마다 다시 읽는다. */
    fun visitedRoadGeoJson(): String =
        IosRouteGeoJson.matchedEdges(trackLocalStore.visitedRoadEdges())

    /**
     * "이미 가본 길" 적립 내역을 다시 읽는다 — **러닝을 시작할 때 한 번** 불러야 한다.
     *
     * 안 부르면 앱을 켠 채 두 번째로 달릴 때 직전 러닝에서 적립된 길이 또 처음 가는 길(금색)로
     * 보인다. Android는 `HomeScreen`의 `LaunchedEffect(isRecordingOrPaused)`가 같은 일을 한다.
     */
    fun refreshExploredSegments() {
        liveRoadClassifier.refreshExploredSegments()
    }

    /**
     * 러닝 중 "오늘 달린 길"을 이미 가본 구간(흰색)/처음 가보는 구간(금색)으로 나눈 GeoJSON.
     *
     * 10초마다 서버 매칭으로 갱신되는 도로 매칭 선([LiveMatchHolder])이 있으면 그걸 쓰고, 아직
     * 없으면(러닝 시작 직후 등) 녹화 중인 원본 GPS 점으로 대신 분류한다. 매칭 선은 GPS와 동떨어진
     * 부분을 잘라낸 **조각 목록**이라 조각마다 따로 분류한다 — 이어 붙이면 잘라낸 구간이 직선으로
     * 되살아난다.
     *
     * 분류 결과의 "새로 탐험한 길" 길이는 [LiveNewRoadHolder]로 바로 넘긴다 — 기록 화면 배지가 같은
     * 값을 읽어서, 지도에서 금색으로 보이는 길과 배지 숫자가 항상 같은 근거를 쓴다. 분류는 전국 구간
     * 인덱스를 훑는 작업이라 화면마다 따로 돌리면 매초 같은 계산을 두 번 하게 된다.
     */
    fun classifyLiveRun(): IosLiveRoadClassifier.Result {
        val livePaths = LiveMatchHolder.state.value ?: listOf(RunvealRecorder.currentFilteredPath())
        val result = liveRoadClassifier.classifyPathsToGeoJson(livePaths)
        LiveNewRoadHolder.publish(result.newRoadMeters)
        return result
    }

    /** 빈 선 레이어 — 러닝 중이 아닐 때 실시간 선을 비우는 데 쓴다. */
    val emptyGeoJson: String get() = IosRouteGeoJson.empty
}
