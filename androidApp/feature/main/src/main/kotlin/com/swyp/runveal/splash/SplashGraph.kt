package com.swyp.runveal.splash

import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import com.swyp.runveal.navigation.RunVealNavigator
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.network.meta.MetaApi
import com.swyp.runveal.network.meta.MetaHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SPLASH_DURATION_MILLIS = 2000L
private const val TAG = "SplashGraph"

internal fun NavGraphBuilder.splashGraph(navigator: RunVealNavigator) {
    composable<RunVealRoute.Main.Splash> {
        LaunchedEffect(Unit) {
            // 앱 실행 시 1번 — docs/api/API.md §3.1/§9. 서버가 꺼져 있거나(§5.7) 느려도 스플래시가
            // 멈추면 안 되니 delay/navigate와 분리된 코루틴으로 fire-and-forget 한다. 지도 버전 비교·
            // 재다운로드는 로컬에 버전을 저장하는 다음 단계에서 붙인다 — 지금은 받아서 MetaHolder에만 둔다.
            launch {
                try {
                    val meta = MetaApi().fetchMeta()
                    MetaHolder.publish(meta)
                    Log.i(TAG, "GET /api/v1/meta ok — mapVersion=${meta.mapVersions.current}")
                } catch (e: Exception) {
                    Log.w(TAG, "GET /api/v1/meta failed", e)
                }
            }

            delay(SPLASH_DURATION_MILLIS)

            // TODO: 로그인 여부 확인 후 로그인 화면/홈으로 분기. 로그인 기능 구현 전까지는 항상 홈으로 이동.
            navigator.navigate(
                route = RunVealRoute.Home.Main,
                navOptions = navOptions {
                    popUpTo<RunVealRoute.Main.Splash> { inclusive = true }
                },
            )
        }

        SplashScreen()
    }
}
