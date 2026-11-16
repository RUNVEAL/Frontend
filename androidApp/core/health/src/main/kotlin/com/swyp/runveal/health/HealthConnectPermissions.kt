package com.swyp.runveal.health

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

    /** 운동 세션과 거리. 경로 없이도 세션 목록을 훑는 데는 이것만 있으면 된다. */
    val session: Set<String> = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
    )

    /** 가져오기가 실제로 동작하려면 필요한 전부(세션 + 모든 경로). */
    val all: Set<String> = session + READ_EXERCISE_ROUTES
}
