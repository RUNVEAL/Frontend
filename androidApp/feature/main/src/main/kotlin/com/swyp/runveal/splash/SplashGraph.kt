package com.swyp.runveal.splash

import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import com.swyp.runveal.core.map.DongOverlayHolder
import com.swyp.runveal.core.map.loadDongOverlayBundle
import com.swyp.runveal.navigation.RunVealNavigator
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.network.meta.MetaApi
import com.swyp.runveal.network.meta.MetaHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val SPLASH_DURATION_MILLIS = 2000L

/** 동 오버레이 사전 생성을 기다려 주는 상한. 넘기면 그냥 홈으로 보내고, 다 만들어지는 대로
 * [DongOverlayHolder]를 통해 지도에 얹힌다 — 동이 늦게 뜨는 건 참을 만하지만 스플래시에 오래
 * 잡아 두는 건 아니다. */
private const val OVERLAY_WARM_UP_TIMEOUT_MILLIS = 8000L
private const val TAG = "SplashGraph"

internal fun NavGraphBuilder.splashGraph(navigator: RunVealNavigator) {
    composable<RunVealRoute.Main.Splash> {
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            // 홈 지도의 동/시군구/시도 오버레이를 여기서 미리 만든다 — 전국 경계 geoJSON 파싱과 동별
            // 탐험률 집계에 몇 초가 걸려서, 홈에 들어간 뒤에 시작하면 빈 지도를 보다가 뒤늦게 동이
            // 떠오른다. /meta와 **같이** 시작한다 — 서버가 죽어 있으면 /meta가 타임아웃까지 10초를
            // 쓰는데, 그 뒤에 시작하면 스플래시가 그만큼 더 길어진다. 서버 지도 파일을 쓸지 번들
            // 에셋으로 갈지는 loadDongOverlayBundle 안에서 MetaHolder를 잠깐 기다려 보고 정한다.
            val overlayWarmUp = launch {
                try {
                    DongOverlayHolder.publish(loadDongOverlayBundle(context, builtForTrackId = null))
                } catch (e: Exception) {
                    Log.w(TAG, "동 오버레이 사전 생성 실패 — 홈에서 다시 시도한다", e)
                }
            }

            // 앱 실행 시 1번 — docs/api/API.md §3.1/§9. 지도 버전 비교·재다운로드는 로컬에 버전을
            // 저장하는 다음 단계에서 붙인다 — 지금은 받아서 MetaHolder에만 둔다.
            val metaSucceeded = try {
                val meta = MetaApi().fetchMeta()
                MetaHolder.publish(meta)
                Log.i(TAG, "GET /api/v1/meta ok — mapVersion=${meta.mapVersions.current}")
                true
            } catch (e: Exception) {
                Log.w(TAG, "GET /api/v1/meta failed", e)
                false
            }

            // /meta가 실패(오프라인·서버 꺼짐, §5.7)하면 거의 즉시 지나가 버리니 그때만 최소 노출
            // 시간을 채운다. 그 뒤엔 오버레이가 준비될 때까지(상한까지) 기다렸다 홈으로 넘어간다.
            if (!metaSucceeded) delay(SPLASH_DURATION_MILLIS)
            withTimeoutOrNull(OVERLAY_WARM_UP_TIMEOUT_MILLIS) { overlayWarmUp.join() }

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
