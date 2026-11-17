package com.sloth.runveal.health

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord

/**
 * Health Connect에서 받아야 하는 권한. 화면은 추후 작업이라 여기서는 **집합만 공개**하고,
 * 실제 요청은 UI가 `PermissionController.createRequestPermissionResultContract()`로 한다.
 *
 * 경로(GPS) 권한이 핵심이다 — Runveal은 좌표를 도로에 매칭해야 하므로 경로가 없으면 거리·시간만
 * 받아도 쓸 데가 없다. Health Connect는 경로를 두 가지로 나눠 둔다.
 *  - `READ_EXERCISE_ROUTE`(단수): 사용자가 **세션 하나씩** 시스템 다이얼로그로 허용. 일괄 가져오기엔 못 쓴다.
 *  - [READ_EXERCISE_ROUTES](복수): 모든 경로를 한 번에. 일괄 가져오기에 필요한 쪽이고, 이걸 쓴다.
 *
 * 다만 복수 권한은 Play 배포 시 **Google의 Health Connect 데이터 접근 승인**이 있어야 한다
 * (기술적 차단이 아니라 정책 심사라 개발 중에는 그대로 동작한다).
 */
object HealthConnectPermissions {
    /** `androidx.health.connect.client`가 상수로 노출하지 않는 복수형 경로 권한. */
    const val READ_EXERCISE_ROUTES = "android.permission.health.READ_EXERCISE_ROUTES"

    /**
     * 운동 세션과 거리. 경로 없이도 세션 목록을 훑는 데는 이것만 있으면 된다 — **없으면 아무것도
     * 못 하는 최소 묶음**이라, 가져오기를 막을지 판단하는 기준도 이 집합이다
     * ([HealthConnectAvailability]).
     */
    val session: Set<String> = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
    )

    /**
     * 30일보다 오래된 기록까지 읽는 권한. 이게 없으면 읽기 권한을 줘도 **지난 30일치만** 올라와서
     * (권한 화면이 그렇게 안내한다) 첫 연동에서 90일을 훑는 게 의미가 없어진다.
     *
     * 경로와 달리 제한 데이터가 아니라 승인 없이 받을 수 있다. 다만 Health Connect가 오래된
     * 버전이면 이 권한을 모르고 넘어가므로, 못 받아도 가져오기 자체는 되게 둔다(범위만 좁아진다).
     */
    val READ_HEALTH_DATA_HISTORY: String = HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY

    /**
     * 권한 화면에서 한 번에 요청하는 전부. [session]에 더해 **받으면 좋은** 둘을 같이 묻는다 —
     * 경로([READ_EXERCISE_ROUTES])와 과거 기록([READ_HEALTH_DATA_HISTORY])은 못 받아도 가져오기를
     * 막지 않는다. 각각 "경로 없는 기록은 건너뜀", "30일치만"으로 범위가 줄 뿐이다.
     */
    val all: Set<String> = session + READ_EXERCISE_ROUTES + READ_HEALTH_DATA_HISTORY
}
