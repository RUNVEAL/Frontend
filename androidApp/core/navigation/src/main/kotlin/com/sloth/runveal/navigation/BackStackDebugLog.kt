package com.sloth.runveal.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.NavGraph

private const val TAG = "RunvealBackStack"

/**
 * **TEMP DEBUG** — 홈에서 뒤로가기를 눌렀을 때 "홈이 사라지고 다시 나오는" 증상이 **백스택 중첩**인지
 * **전환 애니메이션**인지 가리기 위한 진단 로그다. 원인을 확인하면 이 파일과 호출부(`App.kt`,
 * `HomeScreen.kt`)를 같이 지운다.
 *
 * 보는 방법: `adb logcat -s RunvealBackStack`
 *
 * **"이동 가능" 숫자가 판정의 핵심이다.** `NavController`는 백스택에서 그래프 엔트리를 뺀 개수가
 * 2 이상일 때만 뒤로가기를 가로챈다(`destinationCountOnBackStack`). 그래서
 *  - 홈에 돌아온 시점에 그 숫자가 **1**이면 → 중첩이 아니다. 뒤로가기는 이 NavHost가 먹지 않고
 *    바깥으로 넘어가며, 화면이 번쩍이는 건 전환/덮개(`isMapDestination`) 쪽 문제다.
 *  - **2 이상**이면 → 중첩이다. 같은 목적지가 스택에 두 번 들어가 있어서 한 번 더 눌러야 빠져나온다.
 *
 * 그래프 엔트리는 `[graph …]`로 적는다 — 중첩 그래프(`HomeTab.Home`)가 몇 겹 쌓였는지가 같이 보인다.
 */
@Composable
fun BackStackDebugLog(label: String, navController: NavController) {
    LaunchedEffect(navController) {
        navController.currentBackStack.collect { entries ->
            val navigable = entries.count { it.destination !is NavGraph }
            val stack = entries.joinToString(" > ") { entry ->
                // 타입 세이프 라우트의 route는 패키지까지 붙은 클래스 이름이라 뒤 토막만 쓴다.
                val name = entry.destination.route?.substringAfterLast('.')?.substringBefore('/') ?: "?"
                if (entry.destination is NavGraph) "[graph $name]" else name
            }
            // `$navigable개`로 쓰면 한글까지 식별자로 붙어 컴파일이 안 된다 — 중괄호로 끊는다.
            Log.i(TAG, "$label | 이동 가능 ${navigable}개 | $stack")
        }
    }
}
