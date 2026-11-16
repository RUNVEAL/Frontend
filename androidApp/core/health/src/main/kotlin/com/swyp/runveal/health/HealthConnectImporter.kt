package com.swyp.runveal.health

import android.content.Context
import android.util.Log
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
import com.swyp.runveal.external.ImportUnavailableReason
import com.swyp.runveal.external.MatchPointReducer
import com.swyp.runveal.map.GeoMath
import com.swyp.runveal.match.ChunkedMatcher
import com.swyp.runveal.match.ServerRoadMatcher
import com.swyp.runveal.network.match.MatchPurpose
import com.swyp.runveal.network.meta.MetaApi
import com.swyp.runveal.network.meta.MetaHolder
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

    /** 가져오기를 시작할 수 있는 상태인지. 화면이 권한 요청 UI를 띄울지 판단하는 데도 쓴다. */
    suspend fun unavailableReason(): ImportUnavailableReason? {
        if (!source.isAvailable) return ImportUnavailableReason.PROVIDER_NOT_AVAILABLE
        val granted = source.grantedPermissions()
        if (!granted.containsAll(HealthConnectPermissions.session)) {
            return ImportUnavailableReason.PERMISSION_DENIED
        }
        if (HealthConnectPermissions.READ_EXERCISE_ROUTES !in granted) {
            return ImportUnavailableReason.ROUTE_PERMISSION_DENIED
        }
        return null
    }

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

        var imported = 0
        var skipped = 0
        var failed = 0
        candidates.forEachIndexed { index, workout ->
            when (importOne(workout)) {
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

        val result = ImportProgress.Finished(imported = imported, skipped = skipped, failed = failed)
        Log.i(TAG, "가져오기 완료 imported=$imported skipped=$skipped failed=$failed")
        ImportProgressHolder.publish(result)
        return result
    }

    private enum class ImportOutcome { IMPORTED, SKIPPED, FAILED }

    private suspend fun importOne(workout: ExternalWorkout): ImportOutcome {
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

        return runCatching { matchAndRecord(trackId, reduced) }.getOrElse { e ->
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

    private suspend fun matchAndRecord(trackId: String, reduced: List<ExternalWorkoutPoint>): ImportOutcome {
        val points = reduced.map { it.toLatLon() }
        val matcher = ServerRoadMatcher(mapVersion = resolveMapVersion(), purpose = MatchPurpose.IMPORT)
        val result = retryingMatch { ChunkedMatcher.matchInChunks(matcher, points) }

        // 이번 기록이 지나간 구의 구간 인덱스가 없으면 segmentId를 길이·행정동으로 못 바꿔서
        // 집계에서 통째로 빠진다(러닝 종료 흐름과 같은 이유).
        RoadSegmentIndexImporter(context, store.database).importIfNeeded(result.sigunguCodes)
        store.recordMatchResult(
            trackId = trackId,
            matchedPath = result.path,
            segmentIds = result.segmentIds,
            nowEpochMs = System.currentTimeMillis(),
        )
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
