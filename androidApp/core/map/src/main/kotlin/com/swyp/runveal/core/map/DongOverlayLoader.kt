package com.swyp.runveal.core.map

import android.content.Context
import com.swyp.runveal.data.local.RunvealDatabaseProvider
import com.swyp.runveal.data.local.FileStore
import com.swyp.runveal.data.local.TrackLocalStore
import com.swyp.runveal.dong.DongExploreRate
import com.swyp.runveal.dong.DongPolygonPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * 홈 지도에 한 번에 얹는 동/시군구/시도 오버레이 묶음.
 *
 * [builtForTrackId]는 이 묶음을 만들 때 반영돼 있던 마지막 러닝 기록([com.swyp.runveal.result.TrackResult])의
 * ID다(앱 실행 직후처럼 기록 반영이 없었으면 null) — 홈은 이 값으로 "지금 내 기록 상태와 같은
 * 오버레이인지"를 판단해서, 스플래시가 이미 만들어 둔 걸 그대로 쓰고 괜히 다시 만들지 않는다.
 */
data class DongOverlayBundle(
    val dongOverlay: DongOverlayGeoJson,
    val glowPolygonParts: Map<String, List<DongPolygonPart>>,
    val sigunguOverlay: SigunguOverlayGeoJson,
    val sidoOverlay: SidoOverlayGeoJson,
    /** 동별 탐험률(%) — 지도 빛 단계를 정하는 값 그대로다. 홈 요약 카드가 "내 동네" 퍼센트를
     * 여기서 꺼내 쓰므로, 카드 숫자와 지도 밝기가 어긋날 수 없다. 0%인 동은 들어 있지 않다
     * ([DongExploreRate.percents]) — 없으면 0으로 읽으면 된다. */
    val dongPercents: Map<String, Int>,
    /** 동별 "가본 길" 길이(m)와 전체 도로 길이(m) — [dongPercents]의 분자·분모 그대로다. 줌인 헤더의
     * "누적 N km / 전체 N km"가 퍼센트와 어긋나지 않게 같은 값을 쓴다. 분모는 서버 `index.json` →
     * 로컬 구간 인덱스 → 번들 추정치 순으로 정해진다. */
    val walkedLengthsByDong: Map<String, Double>,
    val totalLengthsByDong: Map<String, Double>,
    val builtForTrackId: String?,
    /** 이 오버레이를 만든 경계 데이터의 출처 — `"server:<지도 버전>"`(동 경계가 없으면 `"none"`).
     * 빛 이미지 디스크 캐시 키에 들어가서, 서버가 경계를 고쳐 새 버전으로 배포하면 옛 경계로
     * 만들어 둔 빛 이미지가 자동으로 버려진다(안 그러면 새 테두리와 어긋난 채로 남는다). */
    val boundarySourceKey: String,
) {
    companion object {
        /** 서버 동 경계를 못 받았을 때 — 동/시군구/시도 레이어를 비워 둔다. 지도는 그대로 뜨고
         * 경계만 안 보인다. 다음에 다시 만들 때 서버가 살아 있으면 정상으로 채워진다. */
        fun empty(builtForTrackId: String?): DongOverlayBundle = DongOverlayBundle(
            dongOverlay = DongOverlayGeoJson.EMPTY,
            glowPolygonParts = emptyMap(),
            sigunguOverlay = SigunguOverlayGeoJson.EMPTY,
            sidoOverlay = SidoOverlayGeoJson.EMPTY,
            // 경계를 못 받았으면 탐험률도 비운다 — 홈 요약 카드·줌인 헤더는 0%로 보인다.
            // 분자·분모는 로컬에도 있지만, 서버와 코드 체계가 어긋난 채로 숫자만 띄우면
            // 지도는 안 빛나는데 카드만 8%라고 하는 상태가 된다.
            dongPercents = emptyMap(),
            walkedLengthsByDong = emptyMap(),
            totalLengthsByDong = emptyMap(),
            builtForTrackId = builtForTrackId,
            boundarySourceKey = "none",
        )
    }
}

/**
 * 만들어 둔 오버레이를 스플래시 → 홈으로 건네주는 StateFlow 싱글톤. 전국 경계 geoJSON을 파싱하고
 * 동별 탐험률을 집계하는 데 몇 초가 걸려서, 홈에 들어온 뒤에 시작하면 지도가 한참 비어 있다가
 * 뒤늦게 동이 떠오른다 — 스플래시가 미리 만들어 여기 담아 두면 홈은 첫 프레임부터 그릴 수 있다.
 */
object DongOverlayHolder {
    private val _state = MutableStateFlow<DongOverlayBundle?>(null)
    val state: StateFlow<DongOverlayBundle?> = _state.asStateFlow()

    fun publish(bundle: DongOverlayBundle) {
        _state.value = bundle
    }
}

/**
 * 내 기록 기준 동 오버레이 묶음을 만든다(스플래시의 사전 생성, 러닝 종료 후 홈의 갱신 둘 다 이 함수).
 *
 * 분자는 실제로 달려서 "가본 길"이 된 구간 길이(`explored_segment`), 분모는 그 동 전체 도로 길이다 —
 * 서버 `index.json`을 받았으면 그 값을, 못 받았으면 로컬에 들여온 구간 인덱스 집계를, 그것도 비었으면
 * 번들 추정치를 쓴다. 전국 경계 geoJSON은 읽어서 쓰고 들고 있지 않는다(보관하면 그만큼 메모리를
 * 계속 잡는다).
 */
suspend fun loadDongOverlayBundle(context: Context, builtForTrackId: String?): DongOverlayBundle =
    withContext(Dispatchers.IO) {
        val repository = DongAssetRepository(context)
        val trackLocalStore = TrackLocalStore(RunvealDatabaseProvider.database, FileStore())

        // 동 경계는 서버에서만 온다. 못 받으면(오프라인 등) 동 레이어를 비운 채 돌려준다 —
        // 번들 스냅샷으로 대신 그리면 서버와 코드 체계가 달라 완주율이 조용히 틀어진다.
        val server = repository.syncFromServer() ?: return@withContext DongOverlayBundle.empty(builtForTrackId)

        val walkedLengths = trackLocalStore.walkedLengthByDong()
        val totalLengths = server.totalLengthsByDong.ifEmpty { trackLocalStore.totalLengthByDong() }
        val dongPercents = DongExploreRate.percents(walkedLengths, totalLengths)

        // 테두리 선과 빛 이미지가 같은 정점을 쓰도록 한 번에 받는다([DongOverlayResult]).
        val dong = repository.buildOverlay(dongPercents, server.boundaryGeoJson)

        DongOverlayBundle(
            dongOverlay = dong.overlay,
            glowPolygonParts = dong.polygonPartsByAdmCd2,
            sigunguOverlay = repository.buildSigunguOverlay(dongPercents),
            sidoOverlay = repository.buildSidoOverlay(dongPercents),
            dongPercents = dongPercents,
            walkedLengthsByDong = walkedLengths,
            totalLengthsByDong = totalLengths,
            builtForTrackId = builtForTrackId,
            boundarySourceKey = "server:${server.mapVersion}",
        )
    }
