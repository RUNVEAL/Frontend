package com.swyp.runveal.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ExerciseRouteResult
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.swyp.runveal.external.ExternalWorkout
import com.swyp.runveal.external.ExternalWorkoutPoint
import java.time.Instant

/**
 * Health Connect에서 달리기 세션과 GPS 경로를 읽어 Runveal 모델([ExternalWorkout])로 바꾼다.
 * 중복 확인·도로 매칭·저장은 [HealthConnectImporter]가 맡는다.
 *
 * 트레드밀 달리기(`EXERCISE_TYPE_RUNNING_TREADMILL`)는 제외한다 — 실내라 GPS 경로가 없어서
 * 도로를 채울 수 없고, 가져와도 "탐험한 길"에 기여하지 못한다. 걷기도 같은 이유가 아니라 기준
 * 문제로 뺐다: Runveal은 "달린 길"을 채우는 앱이라 걷기까지 인정하면 기준이 흐려진다.
 * 넓히려면 [IMPORTED_EXERCISE_TYPES]만 고치면 된다.
 */
class HealthConnectWorkoutSource(context: Context) {

    /** Health Connect가 없거나 업데이트가 필요한 기기에서는 null — [isAvailable]로 먼저 확인한다. */
    private val client: HealthConnectClient? =
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
        } else {
            null
        }

    val isAvailable: Boolean get() = client != null

    /** 지금 허용된 권한. 화면이 "권한 요청이 필요한가"를 판단하는 데도 쓴다. */
    suspend fun grantedPermissions(): Set<String> =
        client?.permissionController?.getGrantedPermissions() ?: emptySet()

    /**
     * `(fromEpochMs, untilEpochMs)` 사이의 달리기 세션을 시간순으로. 재개 지점
     * ([com.swyp.runveal.external.ImportCheckpointStore])을 "여기까지 가져왔다"로 쓰기 때문에
     * 오래된 것부터 올라온다.
     */
    suspend fun runningWorkouts(fromEpochMs: Long, untilEpochMs: Long): List<ExternalWorkout> {
        val healthConnect = client ?: return emptyList()
        return healthConnect.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    Instant.ofEpochMilli(fromEpochMs),
                    Instant.ofEpochMilli(untilEpochMs),
                ),
                ascendingOrder = true,
            ),
        ).records
            .filter { it.exerciseType in IMPORTED_EXERCISE_TYPES }
            .map { it.toExternalWorkout() }
    }

    private fun ExerciseSessionRecord.toExternalWorkout(): ExternalWorkout {
        // 경로는 세션에 같이 들어 있지만 권한이 따로다 — 권한이 없으면 ConsentRequired로 오고
        // (기록 자체는 읽힌다), 애초에 경로 없이 기록된 세션은 NoData다. 둘 다 좌표가 없어서
        // 도로 매칭을 할 수 없고, 가져오기는 그 세션을 건너뛴다.
        val locations = (exerciseRouteResult as? ExerciseRouteResult.Data)?.exerciseRoute?.route.orEmpty()
        return ExternalWorkout(
            externalId = metadata.id,
            startedAtEpochMs = startTime.toEpochMilli(),
            finishedAtEpochMs = endTime.toEpochMilli(),
            // 거리는 DistanceRecord를 따로 합산해야 나오는데, 가져온 기록은 매칭된 경로 길이로
            // 대신할 수 있어서 추가 조회를 하지 않는다.
            distanceMeters = null,
            route = locations.map {
                ExternalWorkoutPoint(
                    lat = it.latitude,
                    lon = it.longitude,
                    timeEpochMs = it.time.toEpochMilli(),
                )
            },
        )
    }

    private companion object {
        /** 가져올 운동 종류 — 야외 달리기만. */
        val IMPORTED_EXERCISE_TYPES = setOf(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING)
    }
}
