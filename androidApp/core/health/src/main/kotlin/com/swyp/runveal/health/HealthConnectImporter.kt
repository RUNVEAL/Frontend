package com.swyp.runveal.health

import android.content.Context
import android.util.Log
import com.swyp.runveal.core.map.DongAssetRepository
import com.swyp.runveal.core.map.RoadSegmentIndexImporter
import com.swyp.runveal.data.local.FileStore
import com.swyp.runveal.data.local.RunvealDatabaseProvider
import com.swyp.runveal.data.local.TrackLocalStore
import com.swyp.runveal.data.local.TrackSource
import com.swyp.runveal.external.ExternalWorkout
import com.swyp.runveal.external.ExternalWorkoutPoint
import com.swyp.runveal.external.ImportCheckpointStore
import com.swyp.runveal.external.ImportProgress
import com.swyp.runveal.external.ImportProgressHolder
import com.swyp.runveal.external.ImportResult
import com.swyp.runveal.external.ImportResultHolder
import com.swyp.runveal.external.ImportUnavailableReason
import com.swyp.runveal.external.MatchPointReducer
import com.swyp.runveal.map.GeoMath
import com.swyp.runveal.map.LatLon
import com.swyp.runveal.match.ChunkedMatcher
import com.swyp.runveal.match.MatchedPathFilter
import com.swyp.runveal.match.ServerRoadMatcher
import com.swyp.runveal.network.match.MatchPurpose
import com.swyp.runveal.network.meta.MetaApi
import com.swyp.runveal.network.meta.MetaHolder
import com.swyp.runveal.result.DongRateChanges
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.delay

private const val TAG = "HealthConnectImport"

/** 한 번 실행할 때 처리할 상한. 첫 연동에서 수백 건이 한꺼번에 서버로 가는 걸 막는다 — 남은 건은
 * 다음 실행이 재개 지점부터 이어 간다. */
private const val MAX_WORKOUTS_PER_RUN = 50

/** `/match`(IMPORT) 실패 시 재시도 횟수와 기본 대기 — IMPORT는 서버 전체에서 동시에 1건만
 * 처리돼 503이 더 자주 난다(§3.7). */
private const val MATCH_RETRY_COUNT = 3
private const val MATCH_RETRY_BASE_DELAY_MS = 2_000L

/** 세션 사이 간격. 서버가 IMPORT를 한 건씩만 처리하므로 몰아 보내지 않는다(§3.7). */
private const val BETWEEN_WORKOUTS_DELAY_MS = 500L

private const val MATCH_STATE_FAILED = "FAILED"

/** 재개 지점이 없는 첫 실행에서 훑을 기간 — 90일. 전체 이력을 훑으면 첫 연동에만 수백 건이 걸릴
 * 수 있고, IMPORT는 서버가 한 건씩만 처리한다(§3.7). */
private const val DEFAULT_FIRST_SCAN_WINDOW_MS = 90L * 24 * 60 * 60 * 1000

/**
 * Health Connect 운동 기록을 Runveal 기록으로 가져온다(`docs/api/API.md` §3.7 `purpose: "IMPORT"`).
 * UI는 추후 작업이라, 이 클래스는 [importNewWorkouts]를 불러 주면 끝까지 알아서 진행하고 상태만
 * [ImportProgressHolder]로 내보낸다.
 *
 * 한 건의 흐름은 러닝 종료(`TrackingService.matchAndRecord`)와 같다 — 트랙 row를 먼저 넣고
 * `/match`로 구간 ID를 받아 적립한다. 다른 점은 셋이다.
 *  1. `external_id`로 중복을 막는다(가져오기 전 일괄 조회 + DB UNIQUE 제약).
 *  2. 외부 기록은 GPS 간격을 통제할 수 없어서 [MatchPointReducer]로 §5.1 규칙에 맞게 줄인다.
 *  3. IMPORT는 서버 전체에서 동시 1건만 처리되므로 **한 건씩 순서대로** 보내고, 실패하면 물러
 *     앉았다 다시 시도한다.
 *
 * 경로(GPS)가 없는 세션은 도로 매칭을 할 수 없어 아예 건너뛴다 — 저장해 두면 "탐험 0"인 기록만
 * 쌓이고 나중에도 매칭할 방법이 없다.
 */
class HealthConnectImporter(
    private val context: Context,
    private val source: HealthConnectWorkoutSource = HealthConnectWorkoutSource(context),
    private val store: TrackLocalStore = TrackLocalStore(RunvealDatabaseProvider.database, FileStore()),
) {
    private val checkpoints = ImportCheckpointStore(RunvealDatabaseProvider.database)
    private val availability = HealthConnectAvailability(context, source)

    /** 가져오기를 시작할 수 있는 상태인지 — 판단은 권한 요청 UI와 공용인
     * [HealthConnectAvailability]에 있다. */
    suspend fun unavailableReason(): ImportUnavailableReason? = availability.unavailableReason()

    /**
     * 재개 지점 이후에 끝난 달리기 세션을 가져온다. 이미 들여온 것은 건너뛰고, 경로가 없거나
     * 매칭이 끝까지 실패한 건은 세어 두고 다음으로 넘어간다 — 한 건 때문에 전체가 멈추지 않는다.
     */
    suspend fun importNewWorkouts(): ImportProgress {
        unavailableReason()?.let { reason ->
            val progress = ImportProgress.Unavailable(reason)
            ImportProgressHolder.publish(progress)
            return progress
        }

        ImportProgressHolder.publish(ImportProgress.Scanning)

        val from = checkpoints.importedUntilEpochMs(ImportCheckpointStore.KEY_HEALTH_CONNECT)
            ?: (System.currentTimeMillis() - DEFAULT_FIRST_SCAN_WINDOW_MS)
        val until = System.currentTimeMillis()
        val alreadyImported = store.importedExternalIds(TrackSource.HEALTH_CONNECT)

        val candidates = runCatching { source.runningWorkouts(from, until) }
            .onFailure { Log.w(TAG, "세션 조회 실패", it) }
            .getOrDefault(emptyList())
            .filter { it.externalId !in alreadyImported }
            .take(MAX_WORKOUTS_PER_RUN)

        // 결과 화면의 "상승한 탐험률"은 이번 가져오기 **전체**의 전/후 차이다 — 기록마다 재면
        // 한 동을 여러 기록이 지날 때 같은 동이 여러 줄로 나온다. 그래서 루프 밖에서 한 번만 잰다.
        val beforeWalked = store.walkedLengthByDong()
        val accumulated = ImportAccumulator()

        var imported = 0
        var skipped = 0
        var failed = 0
        candidates.forEachIndexed { index, workout ->
            when (importOne(workout, accumulated)) {
                ImportOutcome.IMPORTED -> imported++
                ImportOutcome.SKIPPED -> skipped++
                ImportOutcome.FAILED -> failed++
            }
            // 처리가 끝난 세션의 종료 시각까지 재개 지점을 전진시킨다. 중간에 앱이 죽어도 여기까지는
            // 다시 훑지 않는다 — 중복 자체는 external_id가 막아 주므로 이 값이 틀어져도 안전하다.
            checkpoints.setImportedUntilEpochMs(
                key = ImportCheckpointStore.KEY_HEALTH_CONNECT,
                epochMs = workout.finishedAtEpochMs,
            )
            ImportProgressHolder.publish(
                ImportProgress.Running(done = index + 1, total = candidates.size, failed = failed),
            )
            if (index != candidates.lastIndex) delay(BETWEEN_WORKOUTS_DELAY_MS)
        }

        publishResult(
            imported = imported,
            skipped = skipped,
            failed = failed,
            beforeWalked = beforeWalked,
            accumulated = accumulated,
        )

        val result = ImportProgress.Finished(imported = imported, skipped = skipped, failed = failed)
        Log.i(TAG, "가져오기 완료 imported=$imported skipped=$skipped failed=$failed")
        ImportProgressHolder.publish(result)
        return result
    }

    /**
     * 결과 화면이 쓸 값을 모아 [ImportResultHolder]에 올린다. 진행 상황([ImportProgressHolder])과
     * 따로 두는 건 경로·동 목록처럼 진행률에는 필요 없는 값까지 담기 때문이다.
     */
    private fun publishResult(
        imported: Int,
        skipped: Int,
        failed: Int,
        beforeWalked: Map<String, Double>,
        accumulated: ImportAccumulator,
    ) {
        val dongStats = if (accumulated.touchedDongAdmCodes.isEmpty()) {
            emptyList()
        } else {
            DongRateChanges.build(
                touchedAdmCodes = accumulated.touchedDongAdmCodes,
                beforeWalked = beforeWalked,
                afterWalked = store.walkedLengthByDong(),
                totalLengths = store.totalLengthByDong(),
                namesByCode = loadDongNames(),
            )
        }

        ImportResultHolder.publish(
            ImportResult(
                imported = imported,
                skipped = skipped,
                failed = failed,
                newRoadMeters = accumulated.newRoadMeters,
                dongStats = dongStats,
                routePaths = accumulated.routePaths.toList(),
                newRoadPaths = accumulated.newRoadPaths.toList(),
            ),
        )
    }

    /** 행정동 코드 → 이름. 러닝 결과 화면(`TrackingService.buildDongStats`)과 같은 계층 파일
     * (150KB)에서 꺼낸다 — 이름 하나 때문에 4.3MB 경계 geoJSON을 파싱하지 않는다. 못 읽으면
     * 탐험률 줄이 비는 것으로 끝나고 가져오기 자체는 이미 끝나 있어서 실패를 삼킨다. */
    private fun loadDongNames(): Map<String, String> = runCatching {
        DongAssetRepository(context).loadRegionHierarchy().sido
            .flatMap { sido -> sido.sigungu.flatMap { it.dongs } }
            .associate { it.code to it.name }
    }.onFailure { Log.w(TAG, "동 이름 읽기 실패 — 탐험률 줄을 비운다", it) }
        .getOrDefault(emptyMap())

    private enum class ImportOutcome { IMPORTED, SKIPPED, FAILED }

    /** 기록 여러 건에 걸쳐 모으는 값 — 결과 화면이 "이번 가져오기"를 한 덩어리로 보여 주기 때문이다. */
    private class ImportAccumulator {
        var newRoadMeters: Double = 0.0
        val touchedDongAdmCodes: MutableSet<String> = mutableSetOf()
        val routePaths: MutableList<List<LatLon>> = mutableListOf()
        val newRoadPaths: MutableList<List<LatLon>> = mutableListOf()
    }

    private suspend fun importOne(workout: ExternalWorkout, accumulated: ImportAccumulator): ImportOutcome {
        val reduced = MatchPointReducer.reduce(workout.route)
        if (reduced.isEmpty()) {
            // 경로가 아예 없거나(권한 거부·실내 기록) 점이 너무 적어 서버가 받지 않는 기록.
            Log.i(TAG, "경로 부족으로 건너뜀 id=${workout.externalId} points=${workout.route.size}")
            return ImportOutcome.SKIPPED
        }

        val trackId = UUID.randomUUID().toString()
        val rawPoints = workout.route
        val distanceMeters = workout.distanceMeters
            ?: GeoMath.totalDistanceMeters(rawPoints.map { it.toLatLon() })

        try {
            store.saveTrack(
                id = trackId,
                startedAtEpochMs = workout.startedAtEpochMs,
                finishedAtEpochMs = workout.finishedAtEpochMs,
                distanceMeters = distanceMeters,
                pointCountFiltered = reduced.size,
                pointCountRaw = rawPoints.size,
                filteredGpx = buildGpx(reduced),
                rawGpx = buildGpx(rawPoints),
                nowEpochMs = System.currentTimeMillis(),
                source = TrackSource.HEALTH_CONNECT,
                externalId = workout.externalId,
            )
        } catch (e: Exception) {
            // external_id UNIQUE 충돌 — 다른 경로로 이미 들어왔다는 뜻이라 실패가 아니다.
            Log.i(TAG, "이미 들여온 기록이라 건너뜀 id=${workout.externalId}", e)
            return ImportOutcome.SKIPPED
        }

        return runCatching { matchAndRecord(trackId, reduced, accumulated) }.getOrElse { e ->
            // 기록 자체는 남긴다 — 거리·시간은 이미 유효하고, 지도 교체 때 REMATCH로 다시 매칭할
            // 대상이 된다(§3.7). 다시 매칭하는 흐름은 아직 없어서 TODO로 남는다.
            Log.w(TAG, "매칭 실패 — FAILED로 남긴다 id=${workout.externalId}", e)
            store.database.trackQueries.updateMatchState(
                match_state = MATCH_STATE_FAILED,
                confidence_score = null,
                unmatched_point_count = null,
                updated_at_epoch_ms = System.currentTimeMillis(),
                id = trackId,
            )
            ImportOutcome.FAILED
        }
    }

    private suspend fun matchAndRecord(
        trackId: String,
        reduced: List<ExternalWorkoutPoint>,
        accumulated: ImportAccumulator,
    ): ImportOutcome {
        val points = reduced.map { it.toLatLon() }
        val matcher = ServerRoadMatcher(mapVersion = resolveMapVersion(), purpose = MatchPurpose.IMPORT)
        val result = retryingMatch { ChunkedMatcher.matchInChunks(matcher, points) }

        // 이번 기록이 지나간 구의 구간 인덱스가 없으면 segmentId를 길이·행정동으로 못 바꿔서
        // 집계에서 통째로 빠진다(러닝 종료 흐름과 같은 이유).
        RoadSegmentIndexImporter(context, store.database).importIfNeeded(result.sigunguCodes)

        // 거르는 규칙은 러닝 종료(`TrackingService.matchAndRecord`)와 **같아야 한다** — 가져온
        // 기록만 다른 기준으로 적립하면 같은 길인데 출처에 따라 탐험률이 달라진다.
        //  - 경로: 실제 GPS와 동떨어진 부분을 잘라내고 조각으로 나눈다.
        //  - 구간: 로컬 구간 인덱스와 대조해 진짜 지나간 부분만 적립한다.
        val matchedPaths = MatchedPathFilter.keepNearRecordedPoints(result.path, points)
        val runPaths = matchedPaths.ifEmpty { listOf(points) }
        val coverages = store.coverageOnPaths(runPaths)

        val summary = store.recordMatchResult(
            trackId = trackId,
            matchedPaths = runPaths,
            coverages = coverages,
            nowEpochMs = System.currentTimeMillis(),
        )

        accumulated.newRoadMeters += summary.newRoadMeters
        accumulated.touchedDongAdmCodes += summary.touchedDongAdmCodes
        accumulated.newRoadPaths += summary.newRoadPaths
        // 결과 지도도 기록 탭 썸네일과 같은 선을 그리도록 **매칭 경로**를 모은다 — 원본 GPS를 그리면
        // 도로를 벗어나서 금색 "새로 탐험한 길"과 어긋나 보인다(러닝 결과 화면과 같은 이유).
        accumulated.routePaths += runPaths

        return ImportOutcome.IMPORTED
    }

    /** IMPORT는 서버가 한 건씩만 처리해서 503이 잦다(§3.7) — 대기를 늘려 가며 몇 번 더 시도한다. */
    private suspend fun <T> retryingMatch(block: suspend () -> T): T {
        var lastError: Throwable? = null
        repeat(MATCH_RETRY_COUNT) { attempt ->
            runCatching { block() }
                .onSuccess { return it }
                .onFailure { error ->
                    lastError = error
                    if (attempt != MATCH_RETRY_COUNT - 1) {
                        delay(MATCH_RETRY_BASE_DELAY_MS * (attempt + 1))
                    }
                }
        }
        throw lastError ?: IllegalStateException("매칭 실패")
    }

    private suspend fun resolveMapVersion(): String =
        MetaHolder.state.value?.mapVersions?.current
            ?: MetaApi().fetchMeta().also { MetaHolder.publish(it) }.mapVersions.current

    /** 앱이 직접 기록한 러닝의 GPX와 같은 형식(`TrackingService.buildGpx`) — 기록 상세·내보내기가
     * 출처에 상관없이 같은 파서를 쓰게 맞춘다. 외부 기록엔 속도가 없어 `<speed>`는 넣지 않는다. */
    private fun buildGpx(points: List<ExternalWorkoutPoint>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<gpx version=\"1.1\" creator=\"RUNVEAL\">\n  <trk>\n    <trkseg>\n")
        points.forEach { point ->
            append("      <trkpt lat=\"${point.lat}\" lon=\"${point.lon}\">")
            append("<time>${Instant.ofEpochMilli(point.timeEpochMs)}</time>")
            append("</trkpt>\n")
        }
        append("    </trkseg>\n  </trk>\n</gpx>\n")
    }
}
