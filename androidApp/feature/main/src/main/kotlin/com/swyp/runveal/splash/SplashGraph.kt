package com.swyp.runveal.splash

import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
// TODO(로그인 도입): resolveStartDestination의 주석을 되살릴 때 같이 푼다.
// import com.swyp.runveal.auth.AuthProvider
// import com.swyp.runveal.auth.state.AuthState
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

            // 어디로 보낼지는 /meta보다 먼저 정해 둔다 — 서버가 죽어 있으면 /meta가 타임아웃까지
            // 10초를 쓰는데, 그 뒤에 판정하면 그만큼 늦어진다.
            val startDestination = resolveStartDestination()
            val goingHome = startDestination == RunVealRoute.Home.Main

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

            if (goingHome) {
                // /meta가 실패(오프라인·서버 꺼짐, §5.7)하면 거의 즉시 지나가 버리니 그때만 최소 노출
                // 시간을 채운다. 그 뒤엔 오버레이가 준비될 때까지(상한까지) 기다렸다 홈으로 넘어간다.
                if (!metaSucceeded) delay(SPLASH_DURATION_MILLIS)
                withTimeoutOrNull(OVERLAY_WARM_UP_TIMEOUT_MILLIS) { overlayWarmUp.join() }
            } else {
                // 다음이 로그인 화면이라 홈 지도 오버레이를 기다리지 않는다 — 지도를 바로 보여줄 것도
                // 아닌데 스플래시에 몇 초를 더 붙잡아 둘 이유가 없다. 사전 생성은 이 화면을 떠날 때
                // 취소되고, 로그인 후 홈에 들어가면 HomeScreen이 (홀더가 비어 있으니) 다시 만든다.
                delay(SPLASH_DURATION_MILLIS)
            }

            navigator.navigate(
                route = startDestination,
                navOptions = navOptions {
                    popUpTo<RunVealRoute.Main.Splash> { inclusive = true }
                },
            )
        }

        SplashScreen()
    }
}

/**
 * 스플래시가 넘어갈 첫 화면.
 *
 * TODO(로그인 도입): **1차 출시는 로그인 없이 나간다** — 지금은 토큰을 보지 않고 무조건 홈으로
 *  보낸다. 로그인을 켤 때는 아래 주석을 되살리고 `AuthProvider`/`AuthState` import도 함께 푼다.
 *  로그인 화면(`RunVealRoute.Sign`)과 인증 레이어는 지운 게 아니라 그대로 있다 — 마이페이지에서
 *  로그아웃하면 여전히 로그인 화면으로 간다.
 *
 *  원래 동작: 로컬에 저장된 토큰만 보고 정한다 — 네트워크도 소셜 SDK도 건드리지 않아서
 *  (AuthViewModel.resolveLocalSession) 빠르고, 스플래시에 구글 계정 선택 시트 같은 로그인 UI가
 *  끼어들지 않는다.
 */
private suspend fun resolveStartDestination(): RunVealRoute {
    return RunVealRoute.Home.Main

    // if (AuthProvider.session.resolveLocalSession() != AuthState.LoggedIn) return RunVealRoute.Sign.SignIn
    // if (AuthProvider.repository.isSignUpCompleted()) return RunVealRoute.Home.Main
    //
    // // TODO(서버 인증) 이 "반쪽 세션" 처리는 가입 완료 여부를 로컬 플래그로만 알 수 있어서 생긴 것이다.
    // //  서버가 신규/기존과 가입 완료 상태를 알려주면 이 분기 자체가 필요 없어진다.
    // // 토큰은 있는데 가입 절차(약관 동의·닉네임)를 안 끝낸 반쪽 세션 — 약관 동의 화면에서 앱을 껐을
    // // 때다. 그대로 홈에 들여보내면 동의 없이 쓰게 되니 로그인부터 다시 받는다. 소셜 SDK가 계정을
    // // 기억하고 있어서 보통 탭 한 번이면 끝나고, 그 로그인이 남은 토큰을 덮어쓴다.
    // // 소셜 로그아웃까지 하지는 않는다 — 카카오 로그아웃은 네트워크를 타서 스플래시를 붙잡는다.
    // AuthProvider.session.markLoggedOut()
    // return RunVealRoute.Sign.SignIn
}
