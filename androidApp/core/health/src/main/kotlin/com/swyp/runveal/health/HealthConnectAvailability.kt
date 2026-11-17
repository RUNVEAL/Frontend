package com.swyp.runveal.health

import android.content.Context
import android.util.Log
import com.swyp.runveal.external.ImportUnavailableReason

private const val TAG = "HealthConnectPerm"

/**
 * Health Connect를 **지금 바로 쓸 수 있는 상태인지**만 본다. 가져오기([HealthConnectImporter])와
 * 권한 요청 UI(`core:permission`의 `rememberHealthConnectPermissionGate`)가 같은 기준을 쓰도록
 * 판단을 한곳에 둔다 — 한쪽만 고치면 "권한 다 받았다"면서 가져오기가 거부되는 식으로 어긋난다.
 *
 * 경로(GPS) 권한을 왜 막는 조건에서 빼는지는 [unavailableReason] 안에 적어 뒀다.
 */
class HealthConnectAvailability(
    context: Context,
    private val source: HealthConnectWorkoutSource = HealthConnectWorkoutSource(context),
) {
    /** 막는 이유가 없으면 null — 그때만 가져오기를 시작할 수 있다. */
    suspend fun unavailableReason(): ImportUnavailableReason? {
        if (!source.isAvailable) return ImportUnavailableReason.PROVIDER_NOT_AVAILABLE

        val granted = source.grantedPermissions()
        Log.i(TAG, "허용된 Health Connect 권한: $granted")

        if (!granted.containsAll(HealthConnectPermissions.session)) {
            return ImportUnavailableReason.PERMISSION_DENIED
        }

        // 경로 권한([HealthConnectPermissions.READ_EXERCISE_ROUTES])은 **막는 조건에서 뺀다.**
        // 일반 권한 다이얼로그로 내려오지 않아서(AndroidX에도 읽기 상수가 없다) Google의 Health
        // Connect 데이터 접근 승인 전에는 받을 방법이 없고, 여기서 막으면 사용자가 운동·거리를
        // 다 허용해도 "권한이 필요해요"에서 더 못 나간다.
        //
        // 대신 경로는 세션마다 확인한다 — 읽을 수 없으면 `ExerciseRouteResult`가 ConsentRequired나
        // NoData로 오고, 그 기록은 "경로가 없어 건너뜀"으로 집계된다(결과 화면의 "경로가 없는
        // 기록 N개"가 그것이다). 승인을 받으면 같은 코드가 그대로 경로를 읽는다.
        return null
    }
}
